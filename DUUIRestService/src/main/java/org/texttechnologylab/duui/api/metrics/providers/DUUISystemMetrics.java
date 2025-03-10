package org.texttechnologylab.duui.api.metrics.providers;

import com.sun.management.OperatingSystemMXBean;
import io.prometheus.client.Gauge;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A class containing system metrics and means to update them.
 *
 * @author Cedric Borkowski.
 */
public class DUUISystemMetrics {

    /**
     * The current cpu load.
     */
    private static final Gauge cpuLoad = Gauge.build()
        .name("duui_cpu_load")
        .help("The current cpu load")
        .register();

    /**
     * The current free memory.
     */
    private static final Gauge memoryFree = Gauge.build()
        .name("duui_memory_free")
        .help("The current cpu load")
        .register();

    /**
     * The total memory.
     */
    private static final Gauge memoryTotal = Gauge.build()
        .name("duui_memory_total")
        .help("The current cpu load")
        .register();

    /**
    * The current virtual memory comitted.
     */
    private static final Gauge memoryVirtualComitted = Gauge.build()
        .name("duui_memory_virtual_comitted")
        .help("The current cpu load")
        .register();

    /**
     * The system monitor.
     */
    private static OperatingSystemMXBean monitor;

    /**
     * Register the metrics and start the update service.
     */
    public static void register() {
        monitor = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
        ScheduledFuture<?> service = Executors
            .newScheduledThreadPool(1)
            .scheduleAtFixedRate(
                DUUISystemMetrics::updateSystemMetrics,
                0,
                5,
                TimeUnit.SECONDS
            );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> service.cancel(true)));
    }

    /**
     * Update the system metrics.
     */
    private static void updateSystemMetrics() {
        cpuLoad.set(monitor.getCpuLoad());
        memoryFree.set(monitor.getFreeMemorySize());
        memoryTotal.set(monitor.getTotalMemorySize());
        memoryVirtualComitted.set(monitor.getCommittedVirtualMemorySize());
    }
}
