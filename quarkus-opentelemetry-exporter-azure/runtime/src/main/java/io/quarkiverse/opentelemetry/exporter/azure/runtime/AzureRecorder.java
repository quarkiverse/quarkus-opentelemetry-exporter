package io.quarkiverse.opentelemetry.exporter.azure.runtime;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AzureRecorder {

    public static final Pattern SEMI_COLON_PATTERN = Pattern.compile(";");

    public Function<SyntheticCreationalContext<AzureMonitorCustomizer>, AzureMonitorCustomizer> createAzureMonitorCustomizer(
            AzureExporterRuntimeConfig runtimeConfig, AzureExporterQuarkusRuntimeConfig quarkusRuntimeConfig) {
        return new Function<>() {
            @Override
            public AzureMonitorCustomizer apply(
                    SyntheticCreationalContext<AzureMonitorCustomizer> objectSyntheticCreationalContext) {
                Optional<String> connectionString = findConnectionString(runtimeConfig, quarkusRuntimeConfig);
                return new AzureMonitorCustomizer(connectionString);
            }
        };
    }

    public Function<SyntheticCreationalContext<AzureEndpointSampler>, AzureEndpointSampler> createSampler(
            AzureExporterRuntimeConfig runtimeConfig, AzureExporterQuarkusRuntimeConfig quarkusRuntimeConfig) {
        return new Function<>() {
            @Override
            public AzureEndpointSampler apply(SyntheticCreationalContext<AzureEndpointSampler> context) {
                Optional<String> connectionString = findConnectionString(runtimeConfig, quarkusRuntimeConfig);
                List<String> dropTargets = new ArrayList<>();
                if (connectionString.isPresent()) {
                    List<String> ingestionUrls = findIngestionUrls(connectionString.get());
                    List<String> statsBeatUrls = Arrays.asList("https://westeurope-5.in.applicationinsights.azure.com/",
                            "https://westus-0.in.applicationinsights.azure.com/");
                    dropTargets.addAll(addTrackPartInUrl(ingestionUrls));
                    dropTargets.addAll(addTrackPartInUrl(statsBeatUrls));
                }
                return new AzureEndpointSampler(dropTargets);
            }
        };
    }

    private static List<String> findIngestionUrls(String connectionString) {
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

    private static Optional<String> findConnectionString(AzureExporterRuntimeConfig azureRuntimeConfig,
            AzureExporterQuarkusRuntimeConfig quarkusRuntimeConfig) {
        return azureRuntimeConfig.connectionString()
                .or(quarkusRuntimeConfig::connectionString);
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
