package org.texttechnologylab.duui.api.controllers.users;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.texttechnologylab.DockerUnifiedUIMAInterface.document_handler.DUUIMinioDocumentHandler;
import org.texttechnologylab.DockerUnifiedUIMAInterface.document_handler.DUUINextcloudDocumentHandler;
import org.texttechnologylab.duui.analysis.document.Provider;
import org.texttechnologylab.duui.api.Main;
import org.texttechnologylab.duui.api.controllers.pipelines.DUUIPipelineController;
import org.texttechnologylab.duui.api.routes.DUUIRequestHelper;
import static org.texttechnologylab.duui.api.routes.DUUIRequestHelper.authenticate;
import static org.texttechnologylab.duui.api.routes.DUUIRequestHelper.badRequest;
import static org.texttechnologylab.duui.api.routes.DUUIRequestHelper.getUserId;
import static org.texttechnologylab.duui.api.routes.DUUIRequestHelper.isNullOrEmpty;
import static org.texttechnologylab.duui.api.routes.DUUIRequestHelper.missingField;
import static org.texttechnologylab.duui.api.routes.DUUIRequestHelper.notFound;
import static org.texttechnologylab.duui.api.routes.DUUIRequestHelper.unauthorized;
import org.texttechnologylab.duui.api.storage.DUUIMongoDBStorage;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuth;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import spark.Request;
import spark.Response;



/**
 * A Controller for database operations related to the users collection.
 *
 * @author Cedric Borkowski
 */
public class DUUIUserController {

    /**
     * The allowed updates for a user.
     */
    private static final Set<String> ALLOWED_UPDATES = Set.of(
        "role",
        "session",
        "name",
        "preferences.tutorial",
        "preferences.language",
        "preferences.notifications",
        "worker_count",
        "connections.key",
        "connections.minio.endpoint",
        "connections.minio.access_key",
        "connections.minio.secret_key",
        "connections.dropbox.access_token",
        "connections.dropbox.refresh_token",
        "connections.mongoDB.uri"
    );


    /**
     * Retrieve the stored user credentials for dropbox (access and refresh token).
     *
     * @param user The user to retrieve the credentials for.
     * @return a {@link Document} containing the credentials.
     */
    public static Document getDropboxCredentials(Document user, String providerId) {
        Document projection = DUUIMongoDBStorage
            .Users()
            .find(Filters.eq(user.getObjectId("_id")))
            .projection(Projections.include("connections.dropbox." + providerId))
            .first();

        if (isNullOrEmpty(projection)) {
            return new Document();
        }

        return projection.get("connections", Document.class).get("dropbox", Document.class)
                .get(providerId, Document.class);
    }

    /**
     * Retrieve the stored user credentials for minio (endpoint access and secret key).
     *
     * @param user The user to retrieve the credentials for.
     * @return a {@link Document} containing the credentials.
     */
    public static Document getMinioCredentials(Document user, String providerId) {
        Document projection = DUUIMongoDBStorage
            .Users()
            .find(Filters.eq(user.getObjectId("_id")))
            .projection(Projections.include("connections.minio." + providerId))
            .first();

        if (isNullOrEmpty(projection)) {
            return new Document();
        }

        return projection.get("connections", Document.class).get("minio", Document.class)
                .get(providerId, Document.class);
    }

    /**
     * Retrieve the stored user credentials for nextcloud (uri, username and password).
     *
     * @param user The user to retrieve the credentials for.
     * @return a {@link Document} containing the credentials.
     */
    public static Document getNextCloudCredentials(Document user, String providerId) {
        Document projection = DUUIMongoDBStorage
                .Users()
                .find(Filters.eq(user.getObjectId("_id")))
                .projection(Projections.include("connections.nextcloud." + providerId))
                .first();

        if (isNullOrEmpty(projection)) {
            return new Document();
        }

        return projection.get("connections", Document.class).get("nextcloud", Document.class)
                .get(providerId, Document.class);
    }

    public static Document getGoogleCredentials(Document user, String providerId) {
        Document projection = DUUIMongoDBStorage
                .Users()
                .find(Filters.eq(user.getObjectId("_id")))
                .projection(Projections.include("connections.google." + providerId))
                .first();


        if (isNullOrEmpty(projection)) {
            return new Document();
        }

        Document credentials = projection.get("connections", Document.class).get("google", Document.class)
                .get(providerId, Document.class);

        try {
            String accessToken = refreshAccessToken(
                    credentials.getString("refresh_token"),
                    Main.config.getGoogleClientId(),
                    Main.config.getGoogleClientSecret());
            credentials.put("access_token", accessToken);
            DUUIMongoDBStorage
                .Users()
                .updateOne(
                    Filters.eq(user.getObjectId("_id")),
                        Updates.set("connections.google." + providerId + ".access_token", accessToken)
                );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return projection.get("connections", Document.class).get("google", Document.class)
                .get(providerId, Document.class);
    }

    /**
     * Get a user by id.
     *
     * @param id the user id (as an {@link ObjectId}).
     * @return a {@link Document} containing the user information.
     */
    public static Document getUserById(ObjectId id) {
        return getUserById(id, new ArrayList<>());
    }

    /**
     * Get a user by id. Fields to be included can be specified.
     *
     * @param id            the user id (as an {@link ObjectId}).
     * @param includeFields the database fields to include.
     * @return a {@link Document} containing the user information.
     */
    public static Document getUserById(ObjectId id, List<String> includeFields) {
        List<String> defaultFields = Arrays.asList("email", "role", "session");

        List<String> mergedFields = new ArrayList<>(includeFields);
        for (String field : defaultFields) {
            if (!includeFields.contains(field)) {
                mergedFields.add(field);
            }
        }

        return DUUIMongoDBStorage
            .Users()
            .find(Filters.eq(id))
            .projection(Projections.include(mergedFields))
            .first();

    }

    /**
     * Get a user by id.
     *
     * @param id the user id.
     * @return a {@link Document} containing the user information.
     */
    public static Document getUserById(String id) {
        return getUserById(new ObjectId(id));
    }

    /**
     * Get a user by id. Fields to be included can be specified.
     *
     * @param id            the user id.
     * @param includeFields the database fields to include.
     * @return a {@link Document} containing the user information.
     */
    public static Document getUserById(String id, List<String> includeFields) {
        return getUserById(new ObjectId(id), includeFields);
    }

    /**
     * Check if a user with the given API key exists.
     *
     * @param authorization the API key.
     * @return the user if it exists.
     */
    public static Document matchApiKey(String authorization) {
        return DUUIMongoDBStorage
            .Users()
            .find(Filters.eq("connections.key", authorization))
            .projection(Projections.exclude("password", "password_reset_token", "reset_token_expiration"))
            .first();
    }

    /**
     * Check if a user with the given session id exists.
     *
     * @param session the session id from the web interface.
     * @return the user if it exists.
     */
    public static Document matchSession(String session) {
        return DUUIMongoDBStorage
            .Users()
            .find(Filters.eq("session", session))
            .projection(Projections.exclude("password", "password_reset_token", "reset_token_expiration"))
            .first();
    }

    /**
     * Check if a user with the given password reset toekn exists.
     *
     * @param token the password reset token set from resetting the password in the web interface.
     * @return the user if it exists.
     */
    public static Document getUserByResetToken(String token) {
        return DUUIMongoDBStorage
            .Users()
            .find(Filters.eq("password_reset_token", token))
            .projection(Projections.include("_id", "email", "reset_token_expiration"))
            .first();
    }

    /**
     * Create and insert a user into the database.
     * TODO: Move request part to {@link org.texttechnologylab.duui.api.routes.users.DUUIUsersRequestHandler}.
     *
     * @return the created user.
     */
    public static String insertOne(Request request, Response response) {
        if (invalidRequestOrigin(request.ip())) {
            response.status(401);
            return "Unauthorized";
        }

        Document body = Document.parse(request.body());

        String email = body.getString("email");
        if (email.isEmpty())
            return missingField(response, "email");

        String password = body.getString("password");
        if (password.isEmpty())
            return missingField(response, "password");

        String role = (String) body.getOrDefault("role", "User");

        if (email.endsWith("@duui.org") || email.endsWith("@texttechnologylab.org")) {
            role = Role.ADMIN;
        }

        Document newUser = new Document("email", email)
            .append("password", password)
            .append("created_at", new Date().toInstant().toEpochMilli())
            .append("role", role)
            .append("worker_count", 200)
            .append("preferences", new Document("tutorial", true)
                .append("language", "english")
                .append("notifications", false))
            .append("session", body.getOrDefault("session", null))
            .append("password_reset_token", null)
            .append("reset_token_expiration", null)
            .append("connections", new Document("key", null)
                .append("worker_count", role.equalsIgnoreCase(Role.ADMIN) ? 10000 : 500)
                .append("dropbox", new Document())
                .append("minio", new Document())
                .append("nextcloud", new Document())
                .append("google", new Document())
            );

        DUUIMongoDBStorage
            .Users()
            .insertOne(newUser);

        DUUIMongoDBStorage.convertObjectIdToString(newUser);
        response.status(200);
        return new Document("user", newUser).toJson();
    }

    /**
     * Upsert a group in the database.
     *
     * @param request Request object.
     * @param response Response object.
     * @return Message.
     */
    public static String upsertGroup(Request request, Response response) {

        Document body = Document.parse(request.body());
        String groupId = request.params("groupId");

        if (DUUIRequestHelper.isNullOrEmpty(groupId)) {
            return DUUIRequestHelper.missingField(response, "groupId");
        }

        Document result = DUUIMongoDBStorage.Globals().findOneAndUpdate(
            Filters.exists("groups"),
            Updates.set("groups." + groupId, body),
            new FindOneAndUpdateOptions()
                .upsert(true)
                .returnDocument(ReturnDocument.AFTER)
        );

        if (result == null) {
            response.status(404);
            return new Document("error", "Failed to insert group").toJson();
        }

        System.out.println("Upserted group: " + groupId + " with data: " + body.toJson());

        response.status(200);
        return new Document("message", "Successfully updated.").toJson();
    }

    /**
     * Delete a group from the database.
     *
     * @param request Request object.
     * @param response Response object.
     * @return Confirmation message.
     */
    public static String deleteGroup(Request request, Response response) {

        String groupId = request.params("groupId");

        if (DUUIRequestHelper.isNullOrEmpty(groupId)) {
            return DUUIRequestHelper.missingField(response, "groupId");
        }

        DeleteResult result = DUUIMongoDBStorage.Globals()
                .deleteOne(Filters.exists("groups." + groupId));

        if (result.wasAcknowledged() && result.getDeletedCount() < 1)
            return DUUIRequestHelper.badRequest(response, "Group " + groupId + " could not be deleted.");

        response.status(204);

        System.out.println("Deleted group: " + groupId);

        return "Successfully deleted group: " + groupId;
    }

    /**
     * Only accessible through admins.
     *
     * Retrieves groups from database. Every group has members which correspond to user emails and
     * labels which this group has access to. When creating a component, a user is restricted to adding
     * public labels or labels contained in one of the users valid groups.
     *
     * @param request Request object.
     * @param response Response object.
     * @return Document containing all groups.
     */
    public static String getGroups(Request request, Response response) {

        Document groupsDoc = DUUIMongoDBStorage.Globals()
            .findOneAndUpdate(
                Filters.exists("groups"),
                Updates.setOnInsert("groups", new Document()),
                new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
            );

        if (groupsDoc == null || !groupsDoc.containsKey("groups")) {
            response.status(404);
            return new Document("error", "Groups document not found or could not be created.").toJson();
        }

        Document groups = groupsDoc.get("groups", Document.class);

        System.out.println("Fetched groups: " + groups.toJson());

        response.status(200);
        return groups.toJson();
    }

    public static String upsertLabel(Request request, Response response) {
        Document body = Document.parse(request.body());
        String labelId = UUID.randomUUID().toString();

        if (request.params(":labelId") != null) {
            DUUIRequestHelper.missingField(response, "labelId");
        }

        Document result = DUUIMongoDBStorage.Globals().findOneAndUpdate(
            Filters.exists("labels"),
            Updates.set("labels." + labelId, body),
            new FindOneAndUpdateOptions()
                .upsert(true)
                .returnDocument(ReturnDocument.AFTER)
        );

        if (result == null) {
            response.status(404);
            return new Document("error", "Failed to insert label").toJson();
        }

        System.out.println("Inserted label: " + labelId + " with data: " + body.toJson());

        response.status(201);
        return new Document("message", "Successfully updated.").toJson();
    }

    public static String deleteLabel(Request request, Response response) {
        String labelId = request.params(":labelId");

        UpdateResult result = DUUIMongoDBStorage.Globals()
                .updateOne(
                        Filters.exists("labels." + labelId),
                        Updates.unset("labels." + labelId)
                );

        if (result.getModifiedCount() == 0) {
            response.status(404);
            return new Document("error", "Label not found").toJson();
        }

        System.out.println("Deleted label: " + labelId);

        response.status(200);
        return new Document("message", "Successfully deleted.").toJson();
    }

    /**
     * Get all labels from the database.
     *
     * @return a list of labels.
     */
    public static String getLabels(Request request, Response response) {

        Document labelsDoc = DUUIMongoDBStorage.Globals()
            .findOneAndUpdate(
                Filters.exists("labels"),
                Updates.setOnInsert("labels", new Document()),
                new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
            );

        if (labelsDoc == null || !labelsDoc.containsKey("labels")) {
            response.status(404);
            return new Document("error", "Labels document not found or could not be created.").toJson();
        }

        Document labels = labelsDoc.get("labels", Document.class);

        System.out.println("Fetched labels: " + labels.toJson());

        response.status(200);
        return labels.toJson();
    }

    /**
     * Get all labels from the database.
     *
     * @return a list of labels.
     */
    public static String getDriverLabels(Request request, Response response) {

        String driver = request.params(":driver");

        // Retrieve role and user from custom headers instead of the request body.
        String role = request.headers("x-user-role");
        String userId = request.headers("x-user-oid");

        if (DUUIRequestHelper.isNullOrEmpty(role)) {
            return DUUIRequestHelper.missingField(response, "role");
        }

        if (DUUIRequestHelper.isNullOrEmpty(userId)) {
            return DUUIRequestHelper.missingField(response, "user");
        }

        return new Document("labels", filterLabelsByDriver(driver, userId, role)).toJson();
    }


    public static List<String> filterLabelsByDriver(String driver, String memberId, String role) {
        Document labels = DUUIMongoDBStorage.Globals().find(Filters.exists("labels")).first();
        if (labels == null) return Collections.emptyList();

        Set<String> permittedLabels = getLabelsByMember(memberId);

        labels = labels.get("labels", Document.class);
        List<String> filteredLabels = labels
            .entrySet().stream()
            .filter(l -> role.equalsIgnoreCase(Role.ADMIN)
                    || ((Document) l.getValue()).getList("groups", String.class).isEmpty()
                    || permittedLabels.contains(l.getKey()))
            .filter(l -> Objects.equals(((Document) l.getValue()).getString("driver"), driver)
                    || Objects.equals(((Document) l.getValue()).getString("driver"), "Any"))
            .map(l -> ((Document) l.getValue()).getString("label"))
            .toList();

        System.out.println("Filtered labels: " + filteredLabels);

        return filteredLabels;
    }


    public static Set<String> getLabelsByMember(String memberId) {
        Set<String> uniqueLabels = new HashSet<>();

        Document doc = DUUIMongoDBStorage.Globals().find(new Document()).first();

        if (doc == null || !doc.containsKey("groups")) {
            return uniqueLabels;
        }

        Document groups = doc.get("groups", Document.class);



        for (Map.Entry<String, Object> entry : groups.entrySet()) {
            Document group = (Document) entry.getValue();

            List<String> members = group.getList("members", String.class);
            if (members != null && members.contains(memberId)) {
                List<String> labels = group.getList("labels", String.class);
                if (labels != null) {
                    uniqueLabels.addAll(labels);
                }
            }
        }

        return uniqueLabels;
    }

    /**
     * Delete a user from the database.
     * TODO: Move request part to {@link org.texttechnologylab.duui.api.routes.users.DUUIUsersRequestHandler}.
     *
     * @return a confirmation message.
     */
    public static String deleteOne(Request request, Response response) {
        if (invalidRequestOrigin(request.ip())) {
            response.status(401);
            return "Unauthorized";
        }

        String id = request.params(":id");

        DUUIMongoDBStorage
            .Users()
            .deleteOne(Filters.eq(new ObjectId(id)));

        DUUIPipelineController.cascade(id);

        response.status(201);
        return new Document("message", "Successfully deleted").toJson();
    }

    /**
     * Check if an email to reset the user password can be sent.
     *
     * @return a confirmation message.
     */
    public static String recoverPassword(Request request, Response response) {
        Document body = Document.parse(request.body());

        String email = body.getString("email");
        if (isNullOrEmpty(email))
            return missingField(response, "email");

        String passwordResetToken = UUID.randomUUID().toString();
        long expiresAt = System.currentTimeMillis() + 60 * 30 * 1000; // 30 Minutes

        DUUIMongoDBStorage
            .Users()
            .findOneAndUpdate(Filters.eq("email", email),
                Updates.combine(
                    Updates.set("password_reset_token", passwordResetToken),
                    Updates.set("reset_token_expiration", expiresAt)));

        sendPasswordResetEmail(email, passwordResetToken);
        response.status(200);
        return new Document("message", "Email has been sent.").toJson();
    }

    /**
     * Send an email to reset a user password.
     * TODO: Implement this method and connect to TTLab email service.
     *
     * @param email              the user's email address
     * @param passwordResetToken the password reset token send with the email.
     */
    private static void sendPasswordResetEmail(String email, String passwordResetToken) {

        String emailContent = String.format("""
                Dear User,
               
                Thank you for registering with us! To complete your registration, please verify your email address by using the code below:
                
                **Your Verification Code:** %s
                
                Simply enter this code in the verification section of our website or app to confirm your email address.
                    
                If you did not create an account using this email address, please disregard this message.
                
                Thank you for being a part of our community!
                
                Best regards,
                
                Texttechnology Lab
                https://www.texttechnologylab.org/
                
                """, passwordResetToken);

        System.out.printf("Sending email to %s with token %s%n", email, passwordResetToken);

        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Reset the password of the user and store the new one.
     *
     * @return a confirmation message and the user email.
     */
    public static String resetPassword(Request request, Response response) {
        Document body = Document.parse(request.body());

        String password = body.getString("password");
        if (password.isEmpty())
            return missingField(response, password);

        String token = body.getString("password_reset_token");
        if (token.isEmpty())
            return unauthorized(response);

        Document user = getUserByResetToken(token);
        if (user.isEmpty())
            return notFound(response);

        long expiresAt = user.getLong("reset_token_expiration");
        if (expiresAt > System.currentTimeMillis())
            return badRequest(response,
                    "You password reset duration has expired. Please request another reset.");

        DUUIMongoDBStorage
                .Users()
                .findOneAndUpdate(
                        Filters.eq(user.getObjectId("_id")),
                        Updates.combine(
                                Updates.set("password", password),
                                Updates.set("password_reset_token", null),
                                Updates.set("reset_token_expiration", null)));

        return new Document("message", "Password has been updated")
                .append("email", user.getString("email")).toJson();
    }

    /**
     * Reset the password of the user and store the new one without.
     *
     * @return a confirmation message and the user email.
     */
    public static String resetPasswordNoToken(Request request, Response response) {
        Document body = Document.parse(request.body());

        String id = request.params(":id");

        String password = body.getString("password");
        if (password.isEmpty())
            return missingField(response, password);

        Document user = getUserById(id);
        if (user.isEmpty())
            return notFound(response);

        DUUIMongoDBStorage
                .Users()
                .findOneAndUpdate(
                        Filters.eq(user.getObjectId("_id")),
                        Updates.combine(
                                Updates.set("password", password),
                                Updates.set("password_reset_token", null),
                                Updates.set("reset_token_expiration", null)));

        return new Document("message", "Password has been updated")
                .append("session", user.getString("session"))
                .append("email", user.getString("email")).toJson();
    }

    /**
     * Fetch the login credentials for a user by email.
     *
     * @return the email and password of the user for logging into the web interface.
     */
    public static String fetchLoginCredentials(Request request, Response response) {
        if (invalidRequestOrigin(request.ip())) {
            response.status(401);
            return "Unauthorized";
        }

        String email = request.params("email");

        Document credentials = DUUIMongoDBStorage
            .Users()
            .find(Filters.eq("email", email.toLowerCase()))
            .projection(Projections.include("email", "password"))
            .first();

        if (isNullOrEmpty(credentials)) {
            return notFound(response);
        }

        DUUIMongoDBStorage.convertObjectIdToString(credentials);

        response.status(200);
        return new Document("credentials", credentials).toJson();
    }

    /**
     * Check if the request origin is valid.
     *
     * @param origin the ip address of the requester.
     * @return if the request origin is a valid ip address.
     */
    private static boolean invalidRequestOrigin(String origin) {
        return false;
//        Set<String> ALLOWED_ORIGINS = Arrays
//            .stream(Main.config.getProperty("ALLOWED_ORIGINS").split(";"))
//            .collect(Collectors.toSet());
//
//        if (ALLOWED_ORIGINS.isEmpty()) return false;
//        return !ALLOWED_ORIGINS.contains(origin);
    }

    /**
     * Update a user given an id and a JSON object with updates.
     *
     * @return the updated user only including the updated and defaults fields.
     */
    public static String updateOne(Request request, Response response) {
        if (invalidRequestOrigin(request.ip())) {
            response.status(401);
            return "Unauthorized";
        }

        Document body = Document.parse(request.body());

        System.out.println("User Update: \n\t" + (body.toJson() == null ? "null" : 
            body.toJson().substring(0, Math.min(100, body.toJson().length()))));

        String id = request.params(":id");

        List<Bson> __updates = new ArrayList<>();
        List<String> __updatedFields = new ArrayList<>();


        for (Map.Entry<String, Object> entry : body.entrySet()) {
            if (!ALLOWED_UPDATES.contains(entry.getKey()) && !entry.getKey().contains("connections")) {
                response.status(400);
                return new Document("error", "Bad Request")
                    .append("message",
                        String.format("%s can not be updated. Allowed updates are %s.",
                            entry.getKey(),
                            String.join(", ", ALLOWED_UPDATES)
                        )).toJson();
            }

            __updates.add(Updates.set(entry.getKey(), entry.getValue()));
            __updatedFields.add(entry.getKey());
        }

        Bson updates = __updates.isEmpty() ? new Document() : Updates.combine(__updates);

        DUUIMongoDBStorage
            .Users()
            .findOneAndUpdate(Filters.eq(new ObjectId(id)), updates);

        Document user = DUUIUserController.getUserById(id, __updatedFields);
        DUUIMongoDBStorage.convertObjectIdToString(user);
        return new Document("user", user).toJson();
    }

    /**
     * Verify and insert new connection details.
     *
     * @return the updated user only including the updated and defaults fields.
     */
    public static String insertNewConnection(Request request, Response response) {
        if (invalidRequestOrigin(request.ip())) {
            response.status(401);
            return "Unauthorized";
        }

        Document body = Document.parse(request.body());

        String id = request.params(":id");
        String provider = request.params(":provider").toLowerCase();

        String connKey = "connections." + provider + ".";
        
        Document connectionDetails = null;

        for (String key : body.keySet()) {
            if (key.startsWith(connKey)) {
                connKey = key;
                connectionDetails = body.get(key, Document.class);
            }
        }

        if (connectionDetails == null) {
            response.status(400);
            return new Document("error", "Bad Request")
                .append("message", "Connection details are missing.").toJson();
        }

        System.out.println(provider + " Connection details: " + connectionDetails.toJson());

        if (provider.equalsIgnoreCase(Provider.MINIO)) {
            try {
                DUUIMinioDocumentHandler handler = new DUUIMinioDocumentHandler(
                        connectionDetails.getString("endpoint"),
                        connectionDetails.getString("access_key"),
                        connectionDetails.getString("secret_key"));
                
            } catch (Exception e) {
                response.status(400);
                return new Document("error", "Bad Request")
                    .append("message", String.format(
                            """
                                Connection details are invalid: %s
                                Endpoint: %s
                                AccessKey: %s
                                SecretKey: %s
                            """, e.getMessage(),
                            connectionDetails.getString("endpoint"),
                            connectionDetails.getString("access_key"),
                            connectionDetails.getString("secret_key")
                        )
                    ).toJson();
            }

        }
        else if (provider.equalsIgnoreCase(Provider.NEXTCLOUD)) {
            try {
                DUUINextcloudDocumentHandler handler =  new DUUINextcloudDocumentHandler(
                    connectionDetails.getString("uri"),
                    connectionDetails.getString("username"),
                    connectionDetails.getString("password"));

            } catch (Exception e) {
                response.status(400);
                return new Document("error", "Bad Request")
                    .append("message", String.format(
                            """
                                Connection details are invalid: %s
                                URI: %s
                                USERNAME: %s
                                PASSWORD: %s
                            """, e.getMessage(),
                            connectionDetails.getString("uri"),
                            connectionDetails.getString("username"),
                            connectionDetails.getString("password")
                        )
                    ).toJson();
            }
        }


        Document update = new Document("$set", new Document(connKey, connectionDetails));
        DUUIMongoDBStorage
                .Users()
                .findOneAndUpdate(Filters.eq(new ObjectId(id)), update);

        Document user = DUUIUserController.getUserById(id, List.of(connKey));
        DUUIMongoDBStorage.convertObjectIdToString(user);
        return connectionDetails.toJson();
    }

    /**
     * Check if the user is authorized.
     *
     * @return the authorized user.
     */
    public static String authorizeUser(Request request, Response response) {
        if (invalidRequestOrigin(request.ip())) {
            response.status(401);
            return "Unauthorized";
        }

        String authorization = request.headers("Authorization");
        Document user = authenticate(authorization);

        if (isNullOrEmpty(user))
            return notFound(response);

        DUUIMongoDBStorage.convertObjectIdToString(user);

        response.status(200);
        return new Document("user", user).toJson();
    }

    /**
     * Retrieve a user from the database given an id.
     *
     * @return a user from the database.
     */
    public static String fetchUser(Request request, Response response) {
        if (invalidRequestOrigin(request.ip())) {
            response.status(401);
            return "Unauthorized";
        }

        String id = request.params(":id");
        Document user = getUserProperties(id);
        if (isNullOrEmpty(user))
            return badRequest(
                response,
                "User not fetchable. Are you logged in or have you provided an API key?");

        DUUIMongoDBStorage.convertObjectIdToString(user);
        return new Document("user", user).toJson();
    }

    /**
     * Retrieve multiple users from the database. This is only possible for admins and is used
     * to manage roles in the web interface.
     *
     * @return a list of users.
     */
    public static String fetchUsers(Request request, Response response) {
        System.out.println("Fetching users");
        // if (invalidRequestOrigin(request.ip())) {
        //     response.status(401);
        //     return "Unauthorized";
        // }

        String userId = getUserId(request);
        
        if (!DUUIRequestHelper.isAdmin(request)) {
            response.status(401);
            return "Unauthorized: You are not an admin.";
        }


        List<Document> users = DUUIMongoDBStorage
            .Users()
            .find(Filters.ne("_id", new ObjectId(userId)))
            .projection(Projections.include("_id", "email", "role", "worker_count"))
            .into(new ArrayList<>());

        if (isNullOrEmpty(users))
            return badRequest(
                response,
                "User not fetchable. Are you logged in or have you provided an API key?");

        users.forEach(DUUIMongoDBStorage::convertObjectIdToString);
        return new Document("users", users).toJson();
    }

    /**
     * Retrieve defaults properties of a user needed for the web interface.
     *
     * @param id the user id.
     * @return a {@link Document} containing the properties.
     */
    private static Document getUserProperties(String id) {

        if (isNullOrEmpty(id)) return new Document();
        return DUUIMongoDBStorage
            .Users()
            .find(Filters.eq(new ObjectId(id)))
            .projection(
                Projections.include("email", "session", "role", "preferences", "connections")
            )
            .first();
    }

    /**
     * Increment or decrement (negative count) to the worker count. Acts as a rate limit for threads.
     *
     * @param id    the user id.
     * @param count the amount of workers to add or subtract.
     */
    public static void addToWorkerCount(String id, int count) {
        DUUIMongoDBStorage
            .Users()
            .findOneAndUpdate(
                Filters.eq(new ObjectId(id)),
                Updates.inc("worker_count", count)
            );
    }

    /**
     * Finish the Dropbox OAuth 2.0 process given a code returned after accepting the connection with DUUI.
     *
     * @return the user with updated dropbox credentials. See {@link DUUIUserController#getDropboxCredentials(Document, String)}.
     */
    public static String finishDropboxOAuthFromCode(Request request, Response response) {
        String code = request.queryParamOrDefault("code", null);
        String providerId = request.queryParamOrDefault("name", null);

        if (isNullOrEmpty(code) || isNullOrEmpty(providerId)) return badRequest(response, "Missing code query parameter");

        DbxRequestConfig config = new DbxRequestConfig("Docker Unified UIMA interface");
        DbxAppInfo info = new DbxAppInfo(Main.config.getDropboxKey(), Main.config.getDropboxSecret());
        DbxWebAuth webAuth = new DbxWebAuth(config, info);

        try {
            DbxAuthFinish finish = webAuth.finishFromCode(code, Main.config.getDropboxRedirectUrl());
            String accessToken = finish.getAccessToken();
            String refreshToken = finish.getRefreshToken();

            System.out.println("Dropbox: \nAccess-Token: " + accessToken.length() + " \nRefresh-Token: " + refreshToken.length());
            
            UpdateResult result = DUUIMongoDBStorage
                .Users()
                .updateOne(
                    Filters.eq(new ObjectId(getUserId(request))),
                    Updates.combine(
                        Updates.set("connections.dropbox." + providerId + ".access_token", accessToken),
                        Updates.set("connections.dropbox." + providerId + ".refresh_token", refreshToken),
                        Updates.set("connections.dropbox." + providerId + ".alias", "")
                    )
                );

            if (result.getModifiedCount() == 1) {
                return getUserById(getUserId(request)).toJson();
            }

            return notFound(response);
        } catch (DbxException e) {
            response.status(500);
            return "Failed";
        }
    }

    /**
        * Finish the Google OAuth 2.0 process given a code returned after accepting the connection with DUUI.
        * @param request the request object.
        * @param response the response object.
        * @return the user with updated google credentials. See {@link DUUIUserController#getGoogleCredentials(Document, String)}.
     */
    public static String finishGoogleOAuthFromCode(Request request, Response response) {
        String code = request.queryParamOrDefault("code", null);
        String providerId = request.queryParamOrDefault("name", null);

        if (isNullOrEmpty(code) || isNullOrEmpty(providerId)) return badRequest(response, "Missing code query parameter");

        try {

            GoogleTokenResponse googleTokenResponse = new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                new JacksonFactory(),
                "https://www.googleapis.com/oauth2/v4/token",
                    Main.config.getGoogleClientId(),
                    Main.config.getGoogleClientSecret(),
                    code,
                    Main.config.getGoogleRedirectUri())
                .execute();

            String accessToken = googleTokenResponse.getAccessToken();
            String refreshToken = googleTokenResponse.getRefreshToken();

            UpdateResult result = DUUIMongoDBStorage
                .Users()
                .updateOne(
                        Filters.eq(new ObjectId(getUserId(request))),
                        Updates.combine(
                            Updates.set("connections.google." + providerId + ".access_token", accessToken),
                            Updates.set("connections.google." + providerId + ".refresh_token", refreshToken),
                            Updates.set("connections.google." + providerId + ".alias", "")
                        )
                );

            if (result.getModifiedCount() == 1) {
                return new Document()
                        .append("access_token", accessToken)
                        .append("refresh_token", refreshToken).toJson();
            }

            return notFound(response);
        } catch (IOException e) {
            response.status(500);
            return "Failed";
        }
    }

    /**
     * Refresh the access token for a user given a refresh token.
     *
     * @param refreshToken the refresh token.
     * @param clientId     the client id.
     * @param clientSecret the client secret.
     * @return the new access token.
     */
    public static String refreshAccessToken(String refreshToken, String clientId, String clientSecret) throws IOException {
        TokenResponse tokenResponse = new GoogleRefreshTokenRequest(new NetHttpTransport(), new JacksonFactory(),
                refreshToken, clientId, clientSecret).setGrantType("refresh_token").execute();

        return tokenResponse.getAccessToken();
    }

    /**
     * Get the Google OAuth 2.0 settings.
     *
     * @return the client id, client secret and redirect uri.
     */
    public static String getGoogleSettings(Request request, Response response) {
        String settings =  new Document()
                .append("key", Main.config.getGoogleClientId())
                .append("secret", Main.config.getGoogleClientSecret())
                .append("url", Main.config.getGoogleRedirectUri())
                .toJson();

        System.out.println("Google settings: " + settings);

        return settings;
    }

    /**
     * Get the Dropbox OAuth 2.0 settings.
     *
     * @return the client id, client secret and redirect uri.
     */
    public static String getDropboxAppSettings(Request request, Response response) {
        return new Document()
            .append("key", Main.config.getDropboxKey())
            .append("secret", Main.config.getDropboxSecret())
            .append("url", Main.config.getDropboxRedirectUrl())
            .toJson();
    }
}
