package io.quarkiverse.opentelemetry.exporter.jaeger.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.test.QuarkusUnitTest;

public class JaegerExporterBadEndpointTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.opentelemetry.tracer.exporter.jaeger.endpoint", "httz://nada:zero")
            .setExpectedException(IllegalArgumentException.class);

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void failStart() {
        Assertions.fail("Test should not be run as deployment should fail");
    }
}
