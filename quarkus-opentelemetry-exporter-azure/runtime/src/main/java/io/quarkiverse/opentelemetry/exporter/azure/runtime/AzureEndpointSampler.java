package io.quarkiverse.opentelemetry.exporter.azure.runtime;

import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.SemanticAttributes;

/**
 * Sampler that drops spans based on the target of the request.
 * Inspired by {@link io.quarkus.opentelemetry.runtime.tracing.DropTargetsSampler}
 */
public class AzureEndpointSampler implements Sampler {

    private final List<String> dropTargets;

    public AzureEndpointSampler(final List<String> dropTargets) {
        this.dropTargets = dropTargets;
    }

    @Override
    public SamplingResult shouldSample(Context context,
            String s,
            String s1,
            SpanKind spanKind,
            Attributes attributes,
            List<LinkData> list) {
        if (spanKind.equals(SpanKind.SERVER)) {
            // HTTP_TARGET was split into url.path and url.query
            String path = attributes.get(SemanticAttributes.URL_PATH);
            String query = attributes.get(SemanticAttributes.URL_QUERY);
            String target = path + (query == null ? "" : "?" + query);

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
        int lastSlashIndex = target.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            if (safeContains(target.substring(0, lastSlashIndex) + "*")
                    || safeContains(target.substring(0, lastSlashIndex) + "/*")) { // check if a wildcard matches
                return true;
            }
        }
        return false;
    }

    private boolean safeContains(String target) {
        return dropTargets.contains(target);
    }
}
