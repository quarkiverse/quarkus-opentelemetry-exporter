package io.quarkiverse.opentelemetry.exporter.azure.runtime;

import java.util.function.Function;

import com.azure.monitor.opentelemetry.exporter.AzureMonitorExporterBuilder;

import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkiverse.opentelemetry.exporter.common.runtime.LateBoundSpanProcessor;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AzureRecorder {

    public Function<SyntheticCreationalContext<LateBoundSpanProcessor>, LateBoundSpanProcessor> createSpanProcessorForAzure(
            AzureExporterConfig.AzureExporterRuntimeConfig runtimeConfig) {
        return new Function<>() {
            @Override
            public LateBoundSpanProcessor apply(SyntheticCreationalContext<LateBoundSpanProcessor> context) {
                try {
                    String azureConnectionString = runtimeConfig.connectionString;
                    SpanExporter azureSpanExporter = new AzureMonitorExporterBuilder().connectionString(azureConnectionString)
                            .buildTraceExporter();
                    return new LateBoundSpanProcessor(BatchSpanProcessor.builder(azureSpanExporter).build());
                } catch (IllegalArgumentException iae) {
                    throw new IllegalStateException("Unable to install OTel Azure Exporter", iae);
                }
            }
        };
    }
}
