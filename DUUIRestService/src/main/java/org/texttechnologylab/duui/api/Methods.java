package org.texttechnologylab.duui.api;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.texttechnologylab.duui.api.controllers.users.DUUIUserController;
import org.texttechnologylab.duui.api.metrics.DUUIMetricsManager;
import org.texttechnologylab.duui.api.metrics.providers.DUUIHTTPMetrics;
import org.texttechnologylab.duui.api.routes.DUUIRequestHelper;
import org.texttechnologylab.duui.api.routes.components.DUUIComponentRequestHandler;
import org.texttechnologylab.duui.api.routes.pipelines.DUUIPipelineRequestHandler;
import org.texttechnologylab.duui.api.routes.processes.DUUIProcessRequestHandler;
import org.texttechnologylab.duui.api.storage.DUUIMongoDBStorage;
import org.texttechnologylab.duui.api.utils.DUUIMailClient;
import org.texttechnologylab.duui.api.websocket.ProcessEventWebSocketHandler;

import java.time.Duration;

import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.options;
import static spark.Spark.path;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.webSocket;

/**
 * A class to set up the Java Spark path groups. This class is only used to increase readability and reduce
 * the amount of code in the main method.
 *
 * @author Cedric Borkowski
 */
public class Methods {

    private static final Logger log = LoggerFactory.getLogger(Methods.class);

    public static void initWS() {
        // Spark's websocket path specs don't support ":id" segments.
        // Match /ws/processes/<id>/events and extract the id in the handler.
        // Servlet spec only allows '*' at the end of a prefix match.
        // Use a catch-all mapping and validate the path in the handler.
        webSocket("/ws/*", ProcessEventWebSocketHandler.class);
    }

    /**
     * Initializes all endpoints including filters and options.
     */
    public static void init() {
        options(
            "/*",
            (request, response) -> {
                String accessControlRequestHeaders = request.headers(
                    "Access-Control-Request-Headers"
                );
                if (accessControlRequestHeaders != null) {
                    response.header(
                        "Access-Control-Allow-Headers",
                        accessControlRequestHeaders
                    );
                }

                String accessControlRequestMethod = request.headers(
                    "Access-Control-Request-Method"
                );
                if (accessControlRequestMethod != null) {
                    response.header(
                        "Access-Control-Allow-Methods",
                        accessControlRequestMethod
                    );
                }
                return "OK";
            }
        );

        before((request, response) -> {
            if (!request.url().endsWith("metrics")) {
                DUUIHTTPMetrics.incrementTotalRequests();
                DUUIHTTPMetrics.incrementActiveRequests();
                String acceptHeader = request.headers("Accept");
                if (acceptHeader == null || acceptHeader.isEmpty()) {
                    acceptHeader = "*/*";
                }
                String originHeader = request.headers("Origin");
                if (originHeader == null || originHeader.isEmpty()) {
                    originHeader = request.ip();
                }
                log.info("{} {} – Origin: {} – Accept: {}", request.requestMethod(), request.pathInfo(), originHeader, acceptHeader);
            }
            response.header("Access-Control-Allow-Origin", "*");
        });

        after((request, response) -> {
            if (!request.url().endsWith("metrics")) {
                DUUIHTTPMetrics.decrementActiveRequests();
            }
        });

        path("/alive", () -> {
            get("", (request, response) -> {
                response.type("text/plain");
                return "OK";
            });
        });


        path("/settings", () -> {
            before("/*", (request, response) -> {
                boolean isAuthorized = DUUIRequestHelper.isAuthorized(request);
                if (!isAuthorized || !DUUIRequestHelper.isAdmin(request)) {
                    log.info("Settings access denied for user: {}", request.userAgent());
                    halt(401, "Unauthorized");
                }
            });

            get("/", Main::getSettings);
            put("/", Main::updateSettings);
        });

        /* Users */
        path("/users", () -> {
            before("/*", (request, response) -> DUUIHTTPMetrics.incrementUsersRequests());

            path("/labels", () -> {
                put("/:labelId", DUUIUserController::upsertLabel);
                delete("/:labelId", DUUIUserController::deleteLabel);
                get("/driver-filter/:driver", DUUIUserController::getDriverLabels);
                get("", DUUIUserController::getLabels);
            });

            path("/registries", () -> {
                get("/", DUUIUserController::getRegistryEndpoints);
                get("/:id", DUUIUserController::getFilteredRegistryEndpoints);
                put("/:id", DUUIUserController::upsertRegistryEndpoint);
                delete("/:id", DUUIUserController::deleteRegistryEndpoint);
            });

            path("/groups", () -> {
                // before("/*", (request, response) -> {
                //     boolean isAuthorized = DUUIRequestHelper.isAdmin(request);
                //     if (!isAuthorized) {
                //         halt(401, "Only admins can access this endpoint.");
                        
                //     }
                // });

                put("/:groupId", DUUIUserController::upsertGroup);
                delete("/:groupId", DUUIUserController::deleteGroup);
                get("", DUUIUserController::getGroups);
            });

            get("/auth", DUUIUserController::authorizeUser);
            get("/:id", DUUIUserController::fetchUser);
            get("", DUUIUserController::fetchUsers);
            post("", DUUIUserController::insertOne);
            put("/:id", DUUIUserController::updateOne);
            put("/:id/reset-password", DUUIUserController::resetPasswordNoToken);
            put("/reset-password", DUUIUserController::resetPassword);
            put("/recover-password", DUUIUserController::recoverPassword);
            delete("/:id", DUUIUserController::deleteOne);
            path("/connections", () -> {
                put("/:id/:provider", DUUIUserController::insertNewConnection);
            });
            path("/auth", () -> {
                get("/login/:email", DUUIUserController::fetchLoginCredentials);
                get("/", DUUIUserController::authorizeUser);
                get("/dropbox", DUUIUserController::getDropboxAppSettings);
                put("/dropbox", DUUIUserController::finishDropboxOAuthFromCode);
                get("/google", DUUIUserController::getGoogleSettings);
                put("/google", DUUIUserController::finishGoogleOAuthFromCode);
            });

            path("/verification", () -> {
                get("/email/:email", (req, res) -> {
                    String email = req.params("email");
                    MongoCollection<Document> usersCollection = DUUIMongoDBStorage.Users();

                    Document userDoc = usersCollection.find(new Document("email", email)).limit(1).first();
                    if (userDoc == null) {
                        res.status(401);
                        return null;
                    }

                    org.bson.types.ObjectId id = userDoc.getObjectId("_id");
                    if (id == null) {
                        res.status(404);
                        return null;
                    }

                    return id.toHexString();
                });

                path("/activation", () -> {
                    post("/send", (req, res) -> {
                        String userId = req.queryParams("userId");
                        String email = req.queryParams("email");

                        String code = DUUIUserController.issueActivationCode(userId, Duration.ofMinutes(10));

                        DUUIMailClient.sendMail(email, "Your activation code", "Code: " + code + "\nExpires in 10 minutes.");

                        res.status(200);
                        return "ok";
                    });

                    post("/verify", (req, res) -> {
                        String userId = req.queryParams("userId");
                        String code = req.queryParams("code");

                        boolean ok = DUUIUserController.verifyActivationCode(userId, code);
                        if (!ok) halt(400, "invalid or expired");

                        res.status(200);
                        return "activated";
                    });
                });

                path("/recovery", () -> {
                    post("/send", (req, res) -> {
                        String userId = req.queryParams("userId");
                        String email = req.queryParams("email");

                        String code = DUUIUserController.issueRecoveryCode(userId, Duration.ofMinutes(10));

                        DUUIMailClient.sendMail(email, "Your password reset code", "Code: " + code + "\nExpires in 10 minutes.");

                        res.status(200);
                        return "ok";
                    });

                    post("/verify", (req, res) -> {
                        String userId = req.queryParams("userId");
                        String code = req.queryParams("code");

                        boolean ok = DUUIUserController.verifyRecoveryCode(userId, code);
                        if (!ok) halt(400, "invalid or expired");

                        res.status(200);
                        return "reset";
                    });
                });

            });
        });

        /* Components */
        path("/components", () -> {
            before("/*", (request, response) -> {
                DUUIHTTPMetrics.incrementComponentsRequests();
                boolean isAuthorized = DUUIRequestHelper.isAuthorized(request);
                if (!isAuthorized) {
                    halt(401, "Unauthorized");
                }
            });
            get("/:id", DUUIComponentRequestHandler::findOne);
            get("", DUUIComponentRequestHandler::findMany);
            post("", DUUIComponentRequestHandler::insertOne);
            put("/:id", DUUIComponentRequestHandler::updateOne);
            delete("/:id", DUUIComponentRequestHandler::deleteOne);
        });

        /* Pipelines */
        path("/pipelines", () -> {
            before("/*", (request, response) -> {
                DUUIHTTPMetrics.incrementPipelinesRequests();
                boolean isAuthorized = DUUIRequestHelper.isAuthorized(request);
                if (!isAuthorized) {
                    halt(401, "Unauthorized");
                }
            });
            get("/:id", DUUIPipelineRequestHandler::findOne);
            get("", DUUIPipelineRequestHandler::findMany);
            post("", DUUIPipelineRequestHandler::insertOne);
            put("/:id", DUUIPipelineRequestHandler::updateOne);
            post("/:id/start", DUUIPipelineRequestHandler::start);
            put("/:id/stop", DUUIPipelineRequestHandler::stop);
            delete("/:id", DUUIPipelineRequestHandler::deleteOne);
        });

        /* Processes */
        path("/processes", () -> {
            before("/*", (request, response) -> {
                DUUIHTTPMetrics.incrementProcessesRequests();
                boolean isAuthorized = DUUIRequestHelper.isAuthorized(request);
                if (!isAuthorized) {
                    halt(401, "Unauthorized");
                }
            });
            get("/folderstructure/:id/:provider/:reset/:providerId", DUUIProcessRequestHandler::getFolderStructure);
            get("/:id", DUUIProcessRequestHandler::findOne);
            get("", DUUIProcessRequestHandler::findMany);
            post("", DUUIProcessRequestHandler::start);
            put("/:id", DUUIProcessRequestHandler::stop);


            delete("/:id", DUUIProcessRequestHandler::deleteOne);
            get("/:id/events", DUUIProcessRequestHandler::findEvents);
            get("/:id/documents", DUUIProcessRequestHandler::findDocuments);
        });

        /* Metrics */
        get("/metrics", (request, response) -> {
                response.type("text/plain");
                return DUUIMetricsManager.export();
            }
        );
        path("/files", () -> {

            get("", Main::downloadFile);
            get("/preprocess", Main::preprocessCas);
            post("", Main::uploadFile);
            get("/local-folder-structure/:reset", Main::getLocalFolderStructure);
            get("/filtered-folder-structure", Main::getFilteredFolderStructure);
        });
    }

}
