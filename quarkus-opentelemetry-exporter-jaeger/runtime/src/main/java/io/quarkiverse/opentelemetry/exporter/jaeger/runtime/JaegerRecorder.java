package io.quarkiverse.opentelemetry.exporter.jaeger.runtime;

import java.net.URI;
import java.util.function.Function;

import io.opentelemetry.exporter.internal.ExporterBuilderUtil;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.quarkiverse.opentelemetry.exporter.common.runtime.LateBoundSpanProcessor;
import io.quarkiverse.opentelemetry.exporter.common.runtime.RemovableLateBoundSpanProcessor;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JaegerRecorder {

    public Function<SyntheticCreationalContext<LateBoundSpanProcessor>, LateBoundSpanProcessor> createBatchSpanProcessorForJaeger(
            JaegerExporterConfig.JaegerExporterRuntimeConfig runtimeConfig) {
        URI baseUri = getBaseUri(runtimeConfig);
        return new Function<>() {
            @Override
            public LateBoundSpanProcessor apply(SyntheticCreationalContext<LateBoundSpanProcessor> context) {
                //Will embrace the default, if no endpoint is set
                if (baseUri == null) {
                    return RemovableLateBoundSpanProcessor.INSTANCE;
                }
                try {
                    JaegerGrpcSpanExporter jaegerSpanExporter = JaegerGrpcSpanExporter.builder()
                            .setEndpoint(baseUri.toString())
                            .setTimeout(runtimeConfig.exportTimeout)
                            .build();
                    return new LateBoundSpanProcessor(BatchSpanProcessor.builder(jaegerSpanExporter).build());
                } catch (IllegalArgumentException iae) {
                    throw new IllegalStateException("Unable to install OTel Jaeger Exporter", iae);
                }
            }
        };
    }

    private URI getBaseUri(JaegerExporterConfig.JaegerExporterRuntimeConfig runtimeConfig) {
        String endpoint = runtimeConfig.endpoint.orElse("").trim();
        if (endpoint.isEmpty()) {
            return null;
        }
        return ExporterBuilderUtil.validateEndpoint(endpoint);
    }
}
