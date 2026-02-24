package io.quarkiverse.opentelemetry.exporter.sentry.beans;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.sentry.opentelemetry.OtelSentrySpanProcessor;

public final class SentrySpanProcessorProducer {
    @SuppressWarnings("unused")
    @ApplicationScoped
    @Produces
    public OtelSentrySpanProcessor produceSentrySpanProcessor() {
        return new OtelSentrySpanProcessor();
    }
}
