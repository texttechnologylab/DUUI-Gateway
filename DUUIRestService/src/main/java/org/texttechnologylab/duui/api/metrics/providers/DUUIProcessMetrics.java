package org.texttechnologylab.duui.api.metrics.providers;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

/**
 * A class containing process related metrics and means to update them.
 *
 * @author Cedric Borkowski.
 */
public class DUUIProcessMetrics {

    /**
     * The number of active processes.
     */
    private static final Gauge activeProcesses = Gauge.build()
        .name("duui_processes_active")
        .help("The number of active processes")
        .register();

    /**
     * The number of completed processes.
     */
    private static final Counter completedProcesses = Counter.build()
        .name("duui_processes_completed")
        .help("The number of completed processes")
        .register();

    /**
      * The number of failed processes.
      */
    private static final Counter failedProcesses = Counter.build()
        .name("duui_processes_failed")
        .help("The number of failed processes")
        .register();

    /**
     * The number of cancelled processes.
     */
    private static final Counter cancelledProcesses = Counter.build()
        .name("duui_processes_cancelled")
        .help("The number of cancelled processes")
        .register();

    /**
     * The number of active threads or workers.
     */
    public static final Gauge activeThreads = Gauge.build()
        .name("duui_threads_active")
        .help("The number of active threads or workers")
        .register();

    /**
     * The number of errors during processing.
     */
    public static final Counter errorCount = Counter.build()
        .name("duui_errors_total")
        .help("The toal number of errors during processing")
        .register();

    /**
     * Register the metrics with the default registry.
     */
    public static void register() {
    }


    /**
        * Increment the number of active processes.
    */
    public static void incrementActiveProcesses() {
        activeProcesses.inc();
    }

    /**
     * Decrement the number of active processes.
     */
    public static void decrementActiveProcesses() {
        activeProcesses.dec();
    }

    /**
     * Increment the number of failed processes.
     */
    public static void incrementFailedProcesses() {
        failedProcesses.inc();
    }


    /**
     * Increment the number of completed processes.
     */
    public static void incrementCompletedProcesses() {
        completedProcesses.inc();
    }

    /**
     * Increment the number of cancelled processes.
     */
    public static void incrementCancelledProcesses() {
        cancelledProcesses.inc();
    }

    /**
     * Increment the number of active threads.
     */
    public static void incrementThreads(double amount) {
        activeThreads.inc(amount);
    }

    /**
     * Decrement the number of active threads.
     */
    public static void decrementThreads(double amount) {
        activeThreads.dec(amount);
    }

    /**
     * Increment the number of errors.
     */
    public static void incrementErrorCount(double amount) {
        errorCount.inc(amount);
    }
}

