package org.texttechnologylab.duui.api.metrics.providers;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;


/**
 * A class containing metrics for requests and means to update them.
 *
 * @author Cedric Borkowski.
 */
public class DUUIHTTPMetrics {

    /**
     * The total number of processed requests.
     */
    private static final Counter requestsTotal = Counter.build()
        .name("duui_requests_total")
        .help("The total number of processed requests")
        .register();

    /**
     * The number of active requests.
     */
    private static final Gauge requestsActive = Gauge.build()
        .name("duui_requests_active")
        .help("The number of active requests")
        .register();

    /**
     * The number of files uploaded.
     */
    private static final Counter filesUploaded = Counter.build()
        .name("duui_files_uploaded_total")
        .help("The number of files uploaded")
        .register();

    /**
    * The total amount of bytes uploaded.
    */
    private static final Counter totalBytesUploaded = Counter.build()
        .name("duui_bytes_uploaded_total")
        .help("The total amount of bytes uploaded")
        .register();

    /**
     * The number of requests to /pipelines.
     */
    private static final Counter requestsPipelines = makeRouteCounter("pipelines");

    /**
     * The number of requests to /processes.
     */
    private static final Counter requestsProcesses = makeRouteCounter("processes");

    /**
     * The number of requests to /components.
     */
    private static final Counter requestsComponents = makeRouteCounter("components");

    /**
     * The number of requests to /users.
     */
    private static final Counter requestsUsers = makeRouteCounter("users");

    /**
     * Register metrics. (Currently empty!)
     */
    public static void register() {
    }

    /**
     * Increment the total number of requests.
     */
    public static void incrementTotalRequests() {
        requestsTotal.inc();
    }


    /**
     * Increment the number of active requests.
     */
    public static void incrementActiveRequests() {
        requestsActive.inc();
    }

    /**
     * Decrement the number of active requests.
     */
    public static void decrementActiveRequests() {
        requestsActive.dec();
    }


    /**
     * Increment the number of requests to /pipelines.
     */
    public static void incrementPipelinesRequests() {
        requestsPipelines.inc();
    }

    /**
     * Increment the number of requests to /processes.
     */
    public static void incrementProcessesRequests() {
        requestsProcesses.inc();
    }

    /**
     * Increment the number of requests to /components.
     */
    public static void incrementComponentsRequests() {
        requestsComponents.inc();
    }

    /**
     * Increment the number of requests to /users.
     */
    public static void incrementUsersRequests() {
        requestsUsers.inc();
    }


    /**
     * Increment the number of files uploaded.
     */
    public static void incrementFilesUploaded(double amount) {
        filesUploaded.inc(amount);
    }


    /**
     * Increment the total amount of bytes uploaded.
     */
    public static void incrementBytesUploaded(double amount) {
        totalBytesUploaded.inc(amount);
    }

    /**
     * Create a counter for a specific route.
     *
     * @param route The route to create a counter for.
     * @return The created counter.
     */
    private static Counter makeRouteCounter(String route) {
        return Counter.build()
            .name(String.format("duui_%s_requests_total", route))
            .help(String.format("The number of requests to /%s", route))
            .register();
    }
}
