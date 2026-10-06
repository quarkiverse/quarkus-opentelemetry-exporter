package io.quarkiverse.opentelemetry.exporter.azure.runtime;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import com.azure.monitor.opentelemetry.autoconfigure.AzureMonitorAutoConfigure;
import com.azure.monitor.opentelemetry.autoconfigure.AzureMonitorAutoConfigureOptions;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReaderBuilder;
import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer;
import io.quarkus.opentelemetry.runtime.config.build.ExporterType;

@Singleton
public class AzureMonitorCustomizer implements AutoConfiguredOpenTelemetrySdkBuilderCustomizer {

    private static final Logger log = Logger.getLogger(AzureMonitorCustomizer.class);

    /**
     * The real Azure Monitor metric exporter is package-private in the Azure SDK, so it is recognized by name.
     */
    public static final String AZURE_METRIC_EXPORTER_CLASS_NAME = "com.azure.monitor.opentelemetry.autoconfigure.AzureMonitorMetricExporter";

    private final Optional<String> connectionString;

    private final Duration metricExportInterval;

    private final Instance<ScheduledExecutorService> managedScheduler;

    /**
     * @param connectionString the Application Insights connection string, if any
     * @param metricExportInterval the periodic metric reader interval, as configured in Quarkus
     *        ({@code quarkus.otel.metric.export.interval}). The OpenTelemetry {@code otel.metric.export.interval}
     *        property is deliberately not read: Quarkus relays the raw value and a unitless number means seconds for
     *        Quarkus but milliseconds for the SDK.
     * @param managedScheduler the Quarkus managed scheduler, used to run the periodic metric reader like Quarkus does.
     *        May be {@code null} or unresolvable, in which case the OpenTelemetry SDK default executor is used.
     */
    public AzureMonitorCustomizer(Optional<String> connectionString, Duration metricExportInterval,
            Instance<ScheduledExecutorService> managedScheduler) {
        this.connectionString = connectionString;
        this.metricExportInterval = metricExportInterval;
        this.managedScheduler = managedScheduler;
    }

    @Override
    public void customize(AutoConfiguredOpenTelemetrySdkBuilder sdkBuilder) {
        if (connectionString.isPresent()) {
            sdkBuilder
                    .addPropertiesSupplier(() -> Collections.singletonMap("applicationinsights.live.metrics.enabled", "false"));
            AzureMonitorAutoConfigure.customize(sdkBuilder, createAutoConfigureOptions(connectionString.get()));
            bindMetricReaderToAzureExporter(sdkBuilder);
        } else {
            sdkBuilder.addPropertiesSupplier(() -> {
                Map<String, String> props = new HashMap<>();
                props.put("applicationinsights.live.metrics.enabled", "false");
                props.put("otel.traces.exporter", ExporterType.NONE.getValue());
                props.put("otel.metrics.exporter", ExporterType.NONE.getValue());
                props.put("otel.logs.exporter", ExporterType.NONE.getValue());
                return props;
            });
            log.info(
                    "Quarkus Opentelemetry Exporter for Microsoft Azure is not enabled because no Application Insights connection string provided.");
        }
    }

    /**
     * The options handed to the Azure Monitor auto-configuration. Overridable for tests that cannot use the Vert.x
     * transport (which requires a running CDI container).
     */
    protected AzureMonitorAutoConfigureOptions createAutoConfigureOptions(String connectionString) {
        return new AzureMonitorAutoConfigureOptions().connectionString(connectionString);
    }

    /**
     * Makes the periodic metric reader use the Azure Monitor metric exporter regardless of the order in which the
     * {@link AutoConfiguredOpenTelemetrySdkBuilderCustomizer} beans are iterated.
     * <p>
     * The Azure SDK registers a no-op marker exporter through the SPI and swaps it for the real exporter in a metric
     * exporter customizer. The Quarkus {@code MetricProviderCustomizer} captures the exporter it observes in that
     * chain and later rebuilds the {@link PeriodicMetricReader} from it (to run the reader on the managed scheduler).
     * When Quarkus is iterated before this customizer, it captures the marker and the real exporter is discarded,
     * silently dropping every metric.
     * <p>
     * The two customizers below are registered right after the Azure ones, so they always observe the real exporter.
     * The exporter and reader customizers are invoked back to back for each configured exporter, which allows pairing
     * the reader with the exporter it was built for. The reader is rebuilt with the Azure exporter: if the Quarkus
     * customizer runs before, its marker-bound reader is replaced; if it runs after, it already captured the real
     * exporter and its rebuild is equivalent. A reader already bound to the Azure exporter is left untouched.
     */
    private void bindMetricReaderToAzureExporter(AutoConfiguredOpenTelemetrySdkBuilder sdkBuilder) {
        AtomicReference<MetricExporter> lastCustomizedExporter = new AtomicReference<>();
        sdkBuilder.addMetricExporterCustomizer((exporter, config) -> {
            lastCustomizedExporter.set(exporter);
            return exporter;
        });
        sdkBuilder.addMetricReaderCustomizer((reader, config) -> {
            MetricExporter exporter = lastCustomizedExporter.getAndSet(null);
            if (!(reader instanceof PeriodicMetricReader) || !isAzureMonitorMetricExporter(exporter)
                    || isBoundToAzureMonitorMetricExporter(reader)) {
                return reader;
            }
            return createPeriodicMetricReader(exporter);
        });
    }

    private MetricReader createPeriodicMetricReader(MetricExporter exporter) {
        PeriodicMetricReaderBuilder builder = PeriodicMetricReader.builder(exporter).setInterval(metricExportInterval);
        if (managedScheduler != null && managedScheduler.isResolvable()) {
            builder.setExecutor(managedScheduler.get());
        }
        log.debugf("Periodic metric reader bound to %s with an export interval of %s", exporter, metricExportInterval);
        return builder.build();
    }

    /**
     * {@link PeriodicMetricReader} does not expose its exporter, but names it in its {@code toString()}. The reader
     * built by the SDK from the Azure exporter (the common case, this customizer being iterated before the Quarkus
     * one) needs no rebuild; should the format change, the reader is simply rebuilt as before.
     */
    private static boolean isBoundToAzureMonitorMetricExporter(MetricReader reader) {
        return reader.toString().contains(AZURE_METRIC_EXPORTER_CLASS_NAME);
    }

    static boolean isAzureMonitorMetricExporter(MetricExporter exporter) {
        return exporter != null && AZURE_METRIC_EXPORTER_CLASS_NAME.equals(exporter.getClass().getName());
    }
}
