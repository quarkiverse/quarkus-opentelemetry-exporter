package io.quarkiverse.opentelemetry.exporter.it;

import jakarta.enterprise.context.ApplicationScoped;

import io.opentelemetry.instrumentation.annotations.WithSpan;

@ApplicationScoped
public class TracedService {
    @WithSpan
    public String call() {
        return "Chained trace";
    }
}
