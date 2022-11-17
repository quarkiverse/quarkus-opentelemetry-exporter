package io.quarkiverse.opentelemetry.exporter.gcp.runtime;

import java.io.IOException;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.google.cloud.opentelemetry.trace.TraceExporter;

import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.quarkus.arc.DefaultBean;

@Singleton
public class GcpSimpleSpanExporterProvider {

    @Produces
    @Singleton
    @DefaultBean
    public SimpleSpanProcessor spanProcessorForGCP() throws IOException {
        TraceExporter traceExporter = TraceExporter.createWithDefaultConfiguration();
        return (SimpleSpanProcessor) SimpleSpanProcessor.create(traceExporter);
    }
}
