package io.quarkiverse.opentelemetry.exporter.sentry.deployment;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.quarkus.test.QuarkusUnitTest;

public class SentryExporterEnabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.sentry.sentry.enabled", "true")
            .overrideConfigKey("quarkus.otel.sentry.dsn", "https://1234@test/1234");

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    Instance<SpanProcessor> sentrySpanProcessorInstance;

    @Test
    void testOpenTelemetryButNoSpanProcessor() {
        Assertions.assertNotNull(openTelemetry);
        Assertions.assertTrue(sentrySpanProcessorInstance.isResolvable());
    }
}
