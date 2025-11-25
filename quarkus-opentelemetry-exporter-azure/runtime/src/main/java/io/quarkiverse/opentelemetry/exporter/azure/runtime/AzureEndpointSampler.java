package io.quarkiverse.opentelemetry.exporter.azure.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.SemanticAttributes;
import io.quarkus.runtime.RuntimeValue;

/**
 * Sampler that drops spans based on the target of the request.
 * Inspired by {@link io.quarkus.opentelemetry.runtime.tracing.DropTargetsSampler}
 */
@Singleton
public class AzureEndpointSampler implements Sampler {

    private final List<String> dropTargets = new ArrayList<>();

    public AzureEndpointSampler() {
        Optional<String> azureConnectionString = ConfigProvider.getConfig()
                .getOptionalValue("applicationinsights.connection.string", String.class);
        Optional<String> quarkusConnectionString = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.otel.azure.applicationinsights.connection.string", String.class);

        Optional<String> connectionString = azureConnectionString.or(() -> quarkusConnectionString);

        if (connectionString.isPresent()) {
            List<String> ingestionUrls = findIngestionUrls(connectionString.get());
            List<String> statsBeatUrls = Arrays.asList("https://westeurope-5.in.applicationinsights.azure.com/",
                    "https://westus-0.in.applicationinsights.azure.com/");
            dropTargets.addAll(addTrackPartInUrl(ingestionUrls));
            dropTargets.addAll(addTrackPartInUrl(statsBeatUrls));
        }
    }

    @Override
    public SamplingResult shouldSample(Context context,
            String s,
            String s1,
            SpanKind spanKind,
            Attributes attributes,
            List<LinkData> list) {
        if (spanKind.equals(SpanKind.CLIENT)) {
            String target = attributes.get(SemanticAttributes.HTTP_URL);
            if (target == null) {
                target = attributes.get(SemanticAttributes.URL_FULL);
            }
            if (shouldDrop(target)) {
                return SamplingResult.drop();
            }
        }
        return Sampler.alwaysOn().shouldSample(context, s, s1, spanKind, attributes, list);
    }

    @Override
    public String getDescription() {
        return "azure-endpoint-sampler";
    }

    private boolean shouldDrop(String target) {
        if ((target == null) || target.isEmpty()) {
            return false;
        }
        if (safeContains(target)) { // check exact match
            return true;
        }
        if (target.charAt(target.length() - 1) == '/') { // check if the path without the ending slash matched
            if (safeContains(target.substring(0, target.length() - 1))) {
                return true;
            }
        }
        return false;
    }

    private boolean safeContains(String target) {
        return dropTargets.contains(target);
    }

    public static final Pattern SEMI_COLON_PATTERN = Pattern.compile(";");

    private static Optional<String> extractIngestionEndpointFrom(String connectionString) {
        Optional<String> ingestionEndpointInConnectionString = SEMI_COLON_PATTERN.splitAsStream(connectionString)
                .filter(element -> element.startsWith("IngestionEndpoint=")).findFirst();
        if (ingestionEndpointInConnectionString.isEmpty()) {
            return Optional.empty();
        }
        return ingestionEndpointInConnectionString
                .map(ingestionPartOfConnectionString -> ingestionPartOfConnectionString.replaceAll("IngestionEndpoint=", ""));
    }

    private static List<String> findIngestionUrls(String connectionString) {
        Optional<String> ingestionEndpoint = extractIngestionEndpointFrom(connectionString);
        if (ingestionEndpoint.isPresent()) {
            return Collections.singletonList(ingestionEndpoint.get());
        }
        return Arrays.asList("https://dc.services.visualstudio.com/",
                "https://rt.services.visualstudio.com/");
    }

    private static Optional<String> findConnectionString(RuntimeValue<AzureExporterRuntimeConfig> azureRuntimeConfig,
            RuntimeValue<AzureExporterQuarkusRuntimeConfig> quarkusRuntimeConfig) {
        return azureRuntimeConfig.getValue().connectionString()
                .or(() -> quarkusRuntimeConfig.getValue().connectionString());
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
