package io.quarkiverse.opentelemetry.exporter.azure.runtime;

import com.azure.monitor.opentelemetry.exporter.AzureMonitorExporterBuilder;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer;

public class AzureMonitorCustomizer implements AutoConfiguredOpenTelemetrySdkBuilderCustomizer {

    private final String azureConnectionString;

    public AzureMonitorCustomizer(String azureConnectionString) {
        this.azureConnectionString = azureConnectionString;
    }

    @Override
    public void customize(AutoConfiguredOpenTelemetrySdkBuilder builder) {
        new AzureMonitorExporterBuilder().connectionString(azureConnectionString)
                .install(builder);
    }
}
