package org.texttechnologylab.duui.api.websocket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bson.Document;
import org.texttechnologylab.duui.api.controllers.pipelines.DUUIPipelineController;
import org.texttechnologylab.duui.api.controllers.processes.DUUIProcessController;
import org.texttechnologylab.duui.api.controllers.users.Role;
import org.texttechnologylab.duui.api.controllers.events.DUUIEventController;
import org.texttechnologylab.duui.api.routes.DUUIRequestHelper;

import java.util.List;
import java.util.Map;

/**
 * WebSocket handler that subscribes a client to live process events.
 *
 * Expected URL: /processes/<id>/events
 * Authentication: "Authorization" header (same as REST API).
 */
@WebSocket
public class ProcessEventWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ProcessEventWebSocketHandler.class);

    private Session session;
    private String processId;

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;

        log.debug(this.session.toString());

        try {
            Map<String, List<String>> params = session.getUpgradeRequest().getParameterMap();
            List<String> processIds = params.get("process_id");
            processId = (processIds != null && !processIds.isEmpty()) ? processIds.get(0) : null;
            String requestPath = session.getUpgradeRequest().getRequestURI() != null
                ? session.getUpgradeRequest().getRequestURI().getPath()
                : null;
            String query = session.getUpgradeRequest().getQueryString();
            log.info("WebSocket connect attempt path={} query={}", requestPath, query);

            // Fallback: derive processId from path /processes/<id>/events
            if (processId == null || processId.isEmpty()) {
                processId = extractProcessId(requestPath);
            }

            String authorization = session
                .getUpgradeRequest()
                .getHeader("Authorization");
            String authSource = "header";

            if (authorization == null || DUUIRequestHelper.isNullOrEmpty(authorization)) {
                String cookieHeader = session
                    .getUpgradeRequest()
                    .getHeader("Cookie");
                authorization = getCookieValue(cookieHeader, "session");
                authSource = "cookie";
            }

            if (authorization == null || DUUIRequestHelper.isNullOrEmpty(authorization)) {
                Map<String, List<String>> param = session.getUpgradeRequest().getParameterMap();
                List<String> tokens = param.get("token");
                authorization = (tokens != null && !tokens.isEmpty()) ? tokens.get(0) : null;
                authSource = "query";
            }

            if (authorization == null || DUUIRequestHelper.isNullOrEmpty(authorization)) {
                log.warn("WebSocket connect denied: missing auth token process_id={}", processId);
                session.close();
                return;
            }

            Document user = DUUIRequestHelper.authenticate(authorization);
            if (DUUIRequestHelper.isNullOrEmpty(user)) {
                log.warn("WebSocket connect denied: unauthorized process_id={} auth_source={}", processId, authSource);
                session.close();
                return;
            }

            if (processId == null || processId.isEmpty()) {
                log.warn("WebSocket connect denied: missing process_id auth_source={}", authSource);
                session.close();
                return;
            }

            String userId = null;
            if (user.getObjectId("_id") != null) {
                userId = user.getObjectId("_id").toHexString();
            } else if (user.getString("oid") != null) {
                userId = user.getString("oid");
            } else if (user.getString("_id") != null) {
                userId = user.getString("_id");
            }
            boolean isAdmin = Role.ADMIN.equalsIgnoreCase(user.getString("role"));

            Document process = DUUIProcessController.findOneById(processId);
            if (DUUIRequestHelper.isNullOrEmpty(process)) {
                log.warn("WebSocket connect denied: process not found process_id={}", processId);
                session.close();
                return;
            }

            Document pipeline = DUUIPipelineController.findOneById(process.getString("pipeline_id"), false);
            if (DUUIRequestHelper.isNullOrEmpty(pipeline)) {
                log.warn("WebSocket connect denied: pipeline not found process_id={}", processId);
                session.close();
                return;
            }

            String pipelineUserId = pipeline.getString("user_id");
            if (pipelineUserId == null && pipeline.getObjectId("user_id") != null) {
                pipelineUserId = pipeline.getObjectId("user_id").toHexString();
            }

            boolean isOwner = pipelineUserId == null || pipelineUserId.isEmpty()
                ? true
                : pipelineUserId.equals(userId);

            if (!isAdmin && !isOwner) {
                log.warn("WebSocket connect denied: forbidden process_id={} auth_source={}", processId, authSource);
                session.close();
                return;
            }

            ProcessWebSocketRegistry
                .getInstance()
                .addSession(processId, session);
            log.info("WebSocket connected for process_id={} auth_source={}", processId, authSource);

            sendEventBacklog(session, processId);
        } catch (Exception exception) {
            log.error("Error during WebSocket connect", exception);
            try {
                session.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static String getCookieValue(String cookieHeader, String name) {
        if (cookieHeader == null || cookieHeader.isEmpty()) return null;
        String[] pairs = cookieHeader.split(";");
        for (String pair : pairs) {
            String trimmed = pair.trim();
            if (trimmed.startsWith(name + "=")) {
                return trimmed.substring(name.length() + 1);
            }
        }
        return null;
    }

    static String extractProcessId(String requestPath) {
        if (requestPath == null || requestPath.isEmpty()) return null;
        String prefix = "/ws/processes/";
        String suffix = "/events";
        int start = requestPath.indexOf(prefix);
        int end = requestPath.lastIndexOf(suffix);
        if (start >= 0 && end > start + prefix.length()) {
            String middle = requestPath.substring(start + prefix.length(), end);
            if (!middle.isEmpty()) {
                return middle;
            }
        }
        return null;
    }

    private void sendEventBacklog(Session session, String processId) {
        try {
            List<Document> events = DUUIEventController.findManyByProcess(processId);
            if (events == null || events.isEmpty()) return;
            for (Document event : events) {
                if (session == null || !session.isOpen()) {
                    return;
                }
                session.getRemote().sendStringByFuture(event.toJson());
            }
        } catch (Exception exception) {
            log.warn("Failed to send WebSocket event backlog for process_id={}", processId, exception);
        }
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        if (processId != null && session != null) {
            ProcessWebSocketRegistry
                .getInstance()
                .removeSession(processId, session);
            log.info("WebSocket closed for process_id={} – {} {}", processId, statusCode, reason);
        }
    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        log.error("WebSocket error", cause);
        if (processId != null && session != null) {
            ProcessWebSocketRegistry
                .getInstance()
                .removeSession(processId, session);
        }
    }
}
