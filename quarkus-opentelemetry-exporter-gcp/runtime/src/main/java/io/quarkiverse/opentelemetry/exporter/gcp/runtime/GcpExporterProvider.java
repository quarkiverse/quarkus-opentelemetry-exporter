package io.quarkiverse.opentelemetry.exporter.gcp.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.quarkus.arc.DefaultBean;

@Singleton
public class GcpExporterProvider {
    @Produces
    @Singleton
    @DefaultBean
    public LateBoundBatchSpanProcessor spanProcessorForGCP() {
        return new LateBoundBatchSpanProcessor();
    }
}
