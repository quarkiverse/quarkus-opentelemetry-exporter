package io.quarkiverse.opentelemetry.exporter.azure.runtime;

import java.util.Collections;

import jakarta.inject.Singleton;

import com.azure.monitor.opentelemetry.autoconfigure.AzureMonitorAutoConfigure;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer;

@Singleton
public class AzureMonitorCustomizer implements AutoConfiguredOpenTelemetrySdkBuilderCustomizer {

    private final String azureConnectionString;

    public AzureMonitorCustomizer(String azureConnectionString) {
        this.azureConnectionString = azureConnectionString;
    }

    @Override
    public void customize(AutoConfiguredOpenTelemetrySdkBuilder sdkBuilder) {
        sdkBuilder.addPropertiesSupplier(() -> Collections.singletonMap("applicationinsights.live.metrics.enabled", "false"));
        AzureMonitorAutoConfigure.customize(sdkBuilder, azureConnectionString);
    }
}
