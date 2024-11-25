package io.quarkiverse.opentelemetry.exporter.sentry.deployment;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.test.QuarkusUnitTest;
import io.sentry.opentelemetry.SentrySpanProcessor;

public class SentryExporterDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.sentry.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    Instance<SentrySpanProcessor> sentrySpanProcessorInstance;

    @Test
    void testOpenTelemetryButNoSpanProcessor() {
        Assertions.assertNotNull(openTelemetry);
        Assertions.assertFalse(sentrySpanProcessorInstance.isResolvable());
    }
}
