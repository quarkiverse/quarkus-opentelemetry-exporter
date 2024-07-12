package io.quarkiverse.opentelemetry.exporter.azure.runtime;

import java.util.*;
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
            AzureExporterRuntimeConfig runtimeConfig, AzureExporterQuarkusRuntimeConfig quarkusRuntimeConfig) {
        return new Function<>() {
            @Override
            public LateBoundSpanProcessor apply(SyntheticCreationalContext<LateBoundSpanProcessor> context) {
                try {
                    String azureConnectionString = findConnectionString(runtimeConfig, quarkusRuntimeConfig);
                    SpanExporter azureSpanExporter = new AzureMonitorExporterBuilder()
                            .connectionString(azureConnectionString)
                            .buildTraceExporter();
                    return new LateBoundSpanProcessor(BatchSpanProcessor.builder(azureSpanExporter).build());
                } catch (IllegalArgumentException iae) {
                    throw new IllegalStateException("Unable to install OTel Azure Exporter", iae);
                }
            }
        };
    }

    public Function<SyntheticCreationalContext<AzureEndpointSampler>, AzureEndpointSampler> createSampler(
            AzureExporterRuntimeConfig runtimeConfig, AzureExporterQuarkusRuntimeConfig quarkusRuntimeConfig) {
        return new Function<>() {
            @Override
            public AzureEndpointSampler apply(SyntheticCreationalContext<AzureEndpointSampler> context) {
                List<String> dropTargets = new ArrayList<>();
                List<String> defaultAzureEndpoints = Arrays.asList("https://dc.services.visualstudio.com/",
                        "https://rt.services.visualstudio.com/", "https://agent.azureserviceprofiler.net/");
                dropTargets.addAll(defaultAzureEndpoints);
                String connectionString = findConnectionString(runtimeConfig, quarkusRuntimeConfig);
                List<String> urlsFromConnectionString = extractUrlsFrom(connectionString);
                dropTargets.addAll(urlsFromConnectionString);
                return new AzureEndpointSampler(dropTargets);
            }
        };
    }

    private static String findConnectionString(AzureExporterRuntimeConfig runtimeConfig,
            AzureExporterQuarkusRuntimeConfig quarkusRuntimeConfig) {
        Optional<String> azureConnectionString = runtimeConfig.connectionString();
        if (azureConnectionString.isPresent()) {
            return azureConnectionString.get();
        }
        return quarkusRuntimeConfig.connectionString()
                .orElseThrow(() -> new IllegalStateException("Azure connection string is missing"));
    }

    private static List<String> extractUrlsFrom(String connectionString) {
        String[] connectionElements = connectionString.split(";");
        return Arrays.stream(connectionElements)
                .map(element -> element.replaceAll("IngestionEndpoint=", ""))
                .map(element -> element.replaceAll("LiveEndpoint=", ""))
                .map(element -> element.replaceAll("ProfilerEndpoint=", ""))
                .filter(element -> element.startsWith("http"))
                .toList();
    }
}
