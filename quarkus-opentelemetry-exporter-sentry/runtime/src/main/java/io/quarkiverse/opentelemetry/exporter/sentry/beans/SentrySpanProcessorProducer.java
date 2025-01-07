package io.quarkiverse.opentelemetry.exporter.sentry.beans;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.sentry.opentelemetry.SentrySpanProcessor;

public final class SentrySpanProcessorProducer {
    @SuppressWarnings("unused")
    @ApplicationScoped
    @Produces
    public SentrySpanProcessor produceSentrySpanProcessor() {
        return new SentrySpanProcessor();
    }
}
