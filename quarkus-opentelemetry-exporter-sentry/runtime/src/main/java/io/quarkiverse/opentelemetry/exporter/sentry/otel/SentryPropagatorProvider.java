package io.quarkiverse.opentelemetry.exporter.sentry.otel;

import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;
import io.sentry.opentelemetry.OtelSentryPropagator;

public class SentryPropagatorProvider implements ConfigurablePropagatorProvider {
    @Override
    public TextMapPropagator getPropagator(ConfigProperties configProperties) {
        return new OtelSentryPropagator();
    }

    @Override
    public String getName() {
        return "sentry";
    }
}
