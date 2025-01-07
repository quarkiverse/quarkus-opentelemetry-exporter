package io.quarkiverse.opentelemetry.exporter.sentry.otel;

import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;
import io.sentry.opentelemetry.SentryPropagator;

public class SentryPropagatorProvider implements ConfigurablePropagatorProvider {
    @Override
    public TextMapPropagator getPropagator(ConfigProperties configProperties) {
        return new SentryPropagator();
    }

    @Override
    public String getName() {
        return "sentry";
    }
}
