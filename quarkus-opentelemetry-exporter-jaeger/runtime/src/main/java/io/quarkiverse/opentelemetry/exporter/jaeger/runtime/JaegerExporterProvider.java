package io.quarkiverse.opentelemetry.exporter.jaeger.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

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
