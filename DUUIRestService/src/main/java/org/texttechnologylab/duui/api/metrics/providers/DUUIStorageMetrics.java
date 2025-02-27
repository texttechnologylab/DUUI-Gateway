package org.texttechnologylab.duui.api.metrics.providers;

import io.prometheus.client.Counter;

/**
 * A class containing database related metrics and means to update them.
 *
 * @author Cedric Borkowski.
 */
public class DUUIStorageMetrics {

    /**
     * The number of CRUD operations on the pipelines collection.
     */
    private static final Counter collectionPipelines = makeCollectionCounter("pipelines");

    /**
     * The number of CRUD operations on the components collection.
     */
    private static final Counter collectionComponents = makeCollectionCounter("components");

    /**
     * The number of CRUD operations on the processes collection.
     */
    private static final Counter collectionProcesses = makeCollectionCounter("processes");

    /**
     * The number of CRUD operations on the documents collection.
     */
    private static final Counter collectionDocuments = makeCollectionCounter("documents");

    /**
     * The number of CRUD operations on the events collection.
     */
    private static final Counter collectionEvents = makeCollectionCounter("events");

    /**
     * The number of CRUD operations on the users collection.
     */
    private static final Counter collectionUsers = makeCollectionCounter("users");

    /**
     * Create a counter for a collection.
     *
     * @param collection The name of the collection.
     * @return The counter.
     */
    private static Counter makeCollectionCounter(String collection) {
        return Counter.build()
            .name(String.format("duui_%s_crud_operations_total", collection))
            .help(String.format("The number of CRUD operations on the %s collection", collection))
            .register();
    }

    /**
     * Register all metrics.
     */
    public static void register() {
    }

    /**
    * Increment the counter for the number of pipelines.
    */
    public static void incrementPipelinesCounter() {
        collectionPipelines.inc();
    }

    /**
     * Increment the counter for the number of components.
     */
    public static void incrementComponentsCounter() {
        collectionComponents.inc();
    }

    /**
     * Increment the counter for the number of processes.
     */
    public static void incrementProcesssesCounter() {
        collectionProcesses.inc();
    }

    /**
    * Increment the counter for the number of documents.
     */
    public static void incrementDocumentsCounter() {
        collectionDocuments.inc();
    }

    /**
     * Increment the counter for the number of events.
     */
    public static void incrementEventsCounter() {
        collectionEvents.inc();
    }

    /**
     * Increment the counter for the number of users.
     */
    public static void incrementUsersCounter() {
        collectionUsers.inc();
    }
}
