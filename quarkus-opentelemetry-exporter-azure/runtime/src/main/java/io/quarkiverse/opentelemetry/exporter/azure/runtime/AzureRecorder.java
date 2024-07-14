package io.quarkiverse.opentelemetry.exporter.azure.runtime;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.azure.monitor.opentelemetry.exporter.AzureMonitorExporterBuilder;

import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkiverse.opentelemetry.exporter.common.runtime.LateBoundSpanProcessor;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AzureRecorder {

    public static final Pattern SEMI_COLON_PATTERN = Pattern.compile(";");

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
                List<String> ingestionUrls = findIngestionUrls(runtimeConfig, quarkusRuntimeConfig);
                List<String> dropTargets = addTrackPartInUrl(ingestionUrls);
                return new AzureEndpointSampler(dropTargets);
            }
        };
    }

    private static List<String> findIngestionUrls(AzureExporterRuntimeConfig runtimeConfig,
            AzureExporterQuarkusRuntimeConfig quarkusRuntimeConfig) {
        String connectionString = findConnectionString(runtimeConfig, quarkusRuntimeConfig);
        Optional<String> ingestionEndpoint = extractIngestionEndpointFrom(connectionString);
        if (ingestionEndpoint.isPresent()) {
            return Collections.singletonList(ingestionEndpoint.get());
        }
        return Arrays.asList("https://dc.services.visualstudio.com/",
                "https://rt.services.visualstudio.com/");
    }

    private static Optional<String> extractIngestionEndpointFrom(String connectionString) {
        Optional<String> ingestionEndpointInConnectionString = SEMI_COLON_PATTERN.splitAsStream(connectionString)
                .filter(element -> element.startsWith("IngestionEndpoint=")).findFirst();
        if (ingestionEndpointInConnectionString.isEmpty()) {
            return Optional.empty();
        }
        return ingestionEndpointInConnectionString
                .map(ingestionPartOfConnectionString -> ingestionPartOfConnectionString.replaceAll("IngestionEndpoint=", ""));
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

    private static List<String> addTrackPartInUrl(List<String> ingestionUrls) {
        return ingestionUrls.stream()
                .map(ingestionUrl -> {
                    if (ingestionUrl.endsWith("/")) {
                        return ingestionUrl;
                    }
                    return ingestionUrl + "/";
                }).map(ingestionUrl -> ingestionUrl + "v2.1/track").toList();
    }
}
