package org.texttechnologylab.duui.api.websocket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jetty.websocket.api.Session;

/**
 * Registry for WebSocket sessions subscribed to process events.
 */
public class ProcessWebSocketRegistry {

    private static final ProcessWebSocketRegistry INSTANCE = new ProcessWebSocketRegistry();

    private ProcessWebSocketRegistry() {
    }

    public static ProcessWebSocketRegistry getInstance() {
        return INSTANCE;
    }

    private final ConcurrentHashMap<String, CopyOnWriteArraySet<Session>> sessions =
        new ConcurrentHashMap<>();

    public void addSession(String processId, Session session) {
        if (processId == null || session == null) return;
        sessions
            .computeIfAbsent(processId, id -> new CopyOnWriteArraySet<>())
            .add(session);
    }

    public void removeSession(String processId, Session session) {
        if (processId == null || session == null) return;
        Set<Session> set = sessions.get(processId);
        if (set == null) return;
        set.remove(session);
        if (set.isEmpty()) {
            sessions.remove(processId);
        }
    }

    public void broadcast(String processId, String message) {
        if (processId == null || message == null) return;
        Set<Session> set = sessions.get(processId);
        if (set == null || set.isEmpty()) return;

        for (Session session : set) {
            if (session.isOpen()) {
                session.getRemote().sendStringByFuture(message);
            }
        }
    }
}

