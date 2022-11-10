package io.quarkiverse.opentelemetry.exporter.gcp.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.quarkus.arc.DefaultBean;

@Singleton
public class GcpSimpleSpanExporterProvider {

    @Produces
    @Singleton
    @DefaultBean
    public LateBoundSimpleSpanProcessor spanProcessorForGCP() {
        return new LateBoundSimpleSpanProcessor();
    }
}
