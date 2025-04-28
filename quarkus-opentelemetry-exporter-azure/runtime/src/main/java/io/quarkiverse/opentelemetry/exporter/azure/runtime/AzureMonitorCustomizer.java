package io.quarkiverse.opentelemetry.exporter.azure.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.quarkus.opentelemetry.runtime.config.build.ExporterType;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import com.azure.monitor.opentelemetry.autoconfigure.AzureMonitorAutoConfigure;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer;

@Singleton
public class AzureMonitorCustomizer implements AutoConfiguredOpenTelemetrySdkBuilderCustomizer {

    private static final Logger log = Logger.getLogger(AzureMonitorCustomizer.class);

    private final Optional<String> connectionString;

    public AzureMonitorCustomizer(Optional<String> connectionString) {
        this.connectionString = connectionString;
    }

    @Override
    public void customize(AutoConfiguredOpenTelemetrySdkBuilder sdkBuilder) {
        if (connectionString.isPresent()) {
            AzureMonitorAutoConfigure.customize(sdkBuilder, connectionString.get());
        } else {
            sdkBuilder.addPropertiesSupplier(() -> {
                Map<String, String> props = new HashMap();
                props.put("otel.traces.exporter", ExporterType.NONE.getValue());
                props.put("otel.metrics.exporter", ExporterType.NONE.getValue());
                props.put("otel.logs.exporter", ExporterType.NONE.getValue());
                return props;
            });
            log.info(
                    "Quarkus Opentelemetry Exporter for Microsoft Azure is not enabled because no Application Insights connection string provided.");
        }
    }
}
