package io.quarkiverse.opentelemetry.exporter.jaeger.runtime;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkiverse.opentelemetry.exporter.common.runtime.LateBoundSpanProcessor;
import io.quarkus.arc.DefaultBean;

@Singleton
public class JaegerExporterProvider {
    @Produces
    @Singleton
    @DefaultBean
    public LateBoundSpanProcessor batchSpanProcessorForJaeger() {
        return new LateBoundSpanProcessor();
    }
}
