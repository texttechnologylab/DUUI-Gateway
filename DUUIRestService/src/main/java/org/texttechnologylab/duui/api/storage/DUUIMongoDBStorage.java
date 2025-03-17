package org.texttechnologylab.duui.api.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.texttechnologylab.duui.api.Config;
import org.texttechnologylab.duui.api.Main;
import org.texttechnologylab.duui.api.metrics.providers.DUUIStorageMetrics;
import org.texttechnologylab.duui.api.routes.DUUIRequestHelper;
import static org.texttechnologylab.duui.api.routes.DUUIRequestHelper.isNullOrEmpty;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

import spark.Request;

/**
 * A utility class for setting up and retrieving information about the database including its collections. Also
 * contains utility methods for database operations.
 *
 * @author Cedric Borkowski
 */
public class DUUIMongoDBStorage {

    /**
     * The {@link MongoClient} instance.
     */
    private static MongoClient mongoClient;

    /**
     * The {@link Config} instance.
     */
    private static Config config;

    /**
     * Replaces the ObjectID object by a plain text representation of the id called
     * oid.
     *
     * @param document The document to convert.
     */
    public static Document convertObjectIdToString(Document document) {
        try {
            String id = document.getObjectId("_id").toString();
            document.remove("_id");
            document.put("oid", id);
        } catch (NullPointerException ignored) {

        }
        return document;
    }

    /**
     * Replaces the ObjectID object by a plain text representation of the id.
     *
     * @param document The document to convert.
     * @param field    The name of the ObjectId field. Usually _id.
     * @param newName  The new name of the field. Usually oid.
     */
    public static Document convertObjectIdToString(Document document, String field, String newName) {
        String id = document.getObjectId(field).toString();
        document.remove(field);
        document.put(newName, id);
        return document;
    }

    /**
     * Convert a MongoDB date object to a timestamp.
     *
     * @param document The document containing the date.
     */
    public static void convertDateToTimestamp(Document document, String fieldName) {
        try {
            Date timestamp = document.get(fieldName, Date.class);
            document.remove(fieldName);
            document.put(fieldName, timestamp.toInstant().toEpochMilli());
        } catch (NullPointerException ignored) {

        }
    }

    /**
     * Retrieve the connection URI to connect to a {@link MongoClient}.
     *
     * @return The connection URI.
     */
    public static String getConnectionURI() {
        return Main.config.getMongoDBConnectionString();
    }

    private DUUIMongoDBStorage() {
    }

    /**
     * Retrieve the existing {@link MongoClient} or instantiate a new one.
     *
     * @return A {@link MongoClient} instance.
     */
    public static MongoClient getClient() {
        if (mongoClient == null) {
            System.out.println("Init MongoConnection");
            //mongodb://[username:password@]host1[:port1][,...hostN[:portN]][/[defaultauthdb][?options]]
            if (getConnectionURI() == null) {
                System.out.println("Using Connection-Config");
                StringBuilder sb = new StringBuilder();
                sb.append("mongodb://");
                sb.append(config.getMongoUser());
                sb.append(":");
                sb.append(config.getMongoPassword());
                sb.append("@");
                sb.append(config.getMongoHost());
                sb.append(":").append(config.getMongoPort());
                sb.append("/?authSource=").append(config.getMongoDatabase());

                mongoClient = MongoClients.create(sb.toString());
            } else {
                System.out.println("Using Connection-URI");
                mongoClient = MongoClients.create(getConnectionURI());
            }
        }
        return mongoClient;
    }

    /**
     * Inject the {@link Config} for the application and initialize a {@link com.mongodb.MongoClient}.
     *
     * @param config the configuration for the application.
     */
    public static void init(Config config) {
        DUUIMongoDBStorage.config = config;
        getClient();
        refactorAllConnections();
    }

    /**
     * Refactor all connections in the database to the new structure.
     */
    public static void refactorAllConnections() {

        MongoCollection<Document> collection = Users();
        for (Document oldDocument : collection.find(Filters.exists("connections"))) {
            Document oldConnections = oldDocument.get("connections", Document.class);

            AtomicInteger modifiedCount = new AtomicInteger();

            String[] services = new String[]{"dropbox", "minio", "nextcloud", "google"};

            // Add empty service document, if not already contained in User document.
            long addedServices = Arrays.stream(services)
                    .map(serviceName -> oldConnections.putIfAbsent(serviceName, new Document()))
                    .filter(Objects::isNull)
                    .count();

            if (addedServices > 0) modifiedCount.getAndIncrement();

            for (Map.Entry<String, Object> entry : oldConnections.entrySet()) {
                String service = entry.getKey();
                boolean isService = Arrays.stream(services)
                        .anyMatch(s -> s.equalsIgnoreCase(service));

                if (!isService) continue;

                Document connectionDetails = (Document) entry.getValue();

                if (connectionDetails.isEmpty()) continue;

                String[] oldDetails = new String[]{"uri", "endpoint", "access_token"};

                boolean isOldStructure = Arrays.stream(oldDetails)
                        .anyMatch(connectionDetails::containsKey);

                if (!isOldStructure) {
                    // Check for null-value connection details and remove them.
                    ArrayList<String> removelist = new ArrayList<>();
                    for (Map.Entry<String, Object> e : connectionDetails.entrySet()) {
                        String key = e.getKey();
                        Object value = e.getValue();

                        if (!(value instanceof Document newConnDetails) || newConnDetails.isEmpty()) {
                            removelist.add(key);
                            modifiedCount.getAndIncrement();
                            continue;
                        }
                        boolean areDetailsEmpty = Arrays.stream(oldDetails)
                                .map(val -> newConnDetails.getOrDefault(val, null))
                                .allMatch(Objects::isNull);

                        if (areDetailsEmpty && !newConnDetails.isEmpty()) {
                            connectionDetails.replace(key, new Document());
                            modifiedCount.getAndIncrement();
                        }
                    }

                    removelist.forEach(connectionDetails::remove);

                } else {
                    // Wrap old connection details structure with a uuid and
                    boolean areDetailsEmpty = Arrays.stream(new Object[]{
                                    connectionDetails.getOrDefault("uri", null),
                                    connectionDetails.getOrDefault("endpoint", null),
                                    connectionDetails.getOrDefault("access_token", null)})
                            .allMatch(Objects::nonNull);

                    // Add alias to connection.
                    if (!areDetailsEmpty) {
                        connectionDetails.putIfAbsent("alias", service + "-detail");
                    }

                    String uuid = UUID.randomUUID().toString();
                    Document wrappedConnection = new Document(uuid, areDetailsEmpty ? new Document() : connectionDetails);
                    oldConnections.replace(service, wrappedConnection);
                    modifiedCount.getAndIncrement();

                }
            }

            if (modifiedCount.get() <= 0) continue;

            Document update = new Document("$set", new Document("connections", oldConnections));

            UpdateResult result = collection.updateOne(
                    Filters.eq("_id", oldDocument.getObjectId("_id")),
                    update,
                    new UpdateOptions().upsert(true)
            );

            System.out.println(result);

        }
    }

    /**
     * Utility functions for fast access to collections in the database.
     *
     * @return A MongoCollection object.
     */
    public static MongoCollection<Document> Pipelines() {
        DUUIStorageMetrics.incrementPipelinesCounter();
        return getClient().getDatabase(config.getMongoDatabase()).getCollection("pipelines");
    }

    /**
     * Utility functions for fast access to collections in the database.
     *
     * @return A MongoCollection object.
     */
    public static MongoCollection<Document> Components() {
        DUUIStorageMetrics.incrementComponentsCounter();
        return getClient().getDatabase(config.getMongoDatabase()).getCollection("components");
    }

    /**
     * Utility functions for fast access to collections in the database.
     *
     * @return A MongoCollection object.
     */
    public static MongoCollection<Document> Users() {
        DUUIStorageMetrics.incrementUsersCounter();
        return getClient().getDatabase(config.getMongoDatabase()).getCollection("users");
    }

    /**
     * Utility functions for fast access to collections in the database.
     *
     * @return A MongoCollection object.
     */
    public static MongoCollection<Document> Documents() {
        DUUIStorageMetrics.incrementDocumentsCounter();
        return getClient().getDatabase(config.getMongoDatabase()).getCollection("documents");
    }

    /**
     * Utility functions for fast access to collections in the database.
     *
     * @return A MongoCollection object.
     */
    public static MongoCollection<Document> Processses() {
        DUUIStorageMetrics.incrementProcesssesCounter();
        return getClient().getDatabase(config.getMongoDatabase()).getCollection("processes");
    }

    /**
     * Utility functions for fast access to collections in the database.
     *
     * @return A MongoCollection object.
     */
    public static MongoCollection<Document> Events() {
        DUUIStorageMetrics.incrementEventsCounter();
        return getClient().getDatabase(config.getMongoDatabase()).getCollection("events");
    }

    /**
     * Utility functions for fast access to collections in the database.
     *
     * @return A MongoCollection object.
     */
    public static MongoCollection<Document> Globals() {

        MongoCollection<Document> collection = getClient().getDatabase(config.getMongoDatabase())
                .getCollection("globals");

        Document existingDocument = collection.find(Filters.exists("labels")).first();

        if (existingDocument == null) {
            Document initialData = new Document("labels", new Document());
            collection.insertOne(initialData);
        }

        return collection;
    }

    /**
     * Combine a Document containing key value pairs representing an update action
     * into a merged BSON object.
     *
     * @param collection     The collection on which to perform the update
     * @param filter         A set of filters so that only matching documents are
     *                       updated.
     * @param updates        The Document containing the updates.
     * @param allowedUpdates A Set of field names that are allowed to be updated.
     * @param skipInvalid    Wether to skip invalid keys or return when encountered.
     *                       Default true.
     */
    public static void updateDocument(MongoCollection<Document> collection,
                                      Bson filter,
                                      Document updates,
                                      Set<String> allowedUpdates,
                                      boolean skipInvalid) {

        List<Bson> __updates = new ArrayList<>();
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            if (!allowedUpdates.contains(entry.getKey())) {
                if (skipInvalid)
                    continue;
                else
                    return;
            }

            __updates.add(Updates.set(entry.getKey(), entry.getValue()));
        }

        collection.updateOne(
            filter,
            __updates.isEmpty()
                ? new Document()
                : Updates.combine(__updates));

    }

    /**
     * See {@link #updateDocument(MongoCollection, Bson, Document, Set, boolean)}
     */
    public static void updateDocument(MongoCollection<Document> collection,
                                      Bson filter,
                                      Document updates,
                                      Set<String> allowedUpdates) {
        updateDocument(collection, filter, updates, allowedUpdates, true);
    }

    /**
     * For a given filter queryParameter extract the string from the request query
     * parameters (queryParameter=A;B;C;D...) and
     * return a {@link List} of filter names
     *
     * @param request        the request object.
     * @param queryParameter the queryParameter of the query parameter
     * @return a list of Strings to filter by.
     */
    public static List<String> getFilterOrDefault(Request request, String queryParameter) {
        String filter = request.queryParamOrDefault(queryParameter, "Any");
        if (isNullOrEmpty(filter))
            return List.of("Any");

        return Stream.of(filter.split(";"))
            .map(DUUIRequestHelper::toTitleCase)
            .toList();
    }

}
