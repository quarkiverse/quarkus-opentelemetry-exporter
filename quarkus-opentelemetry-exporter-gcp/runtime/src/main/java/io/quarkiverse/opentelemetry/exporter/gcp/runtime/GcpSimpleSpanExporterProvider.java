package io.quarkiverse.opentelemetry.exporter.gcp.runtime;

import io.quarkus.arc.DefaultBean;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class GcpSimpleSpanExporterProvider {

    @Produces
    @Singleton
    @DefaultBean
    public LateBoundSimpleSpanProcessor spanProcessorForGCP() {
        return new LateBoundSimpleSpanProcessor();
    }
}
