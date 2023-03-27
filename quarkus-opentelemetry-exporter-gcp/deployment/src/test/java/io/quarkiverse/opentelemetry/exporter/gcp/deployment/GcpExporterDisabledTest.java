package io.quarkiverse.opentelemetry.exporter.gcp.deployment;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkiverse.opentelemetry.exporter.common.runtime.LateBoundSpanProcessor;
import io.quarkus.test.QuarkusUnitTest;

public class GcpExporterDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.opentelemetry.tracer.exporter.gcp.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    Instance<LateBoundSpanProcessor> lateBoundSpanProcessorInstance;

    @Test
    void testOpenTelemetryButNoBatchSpanProcessor() {
        Assertions.assertNotNull(openTelemetry);
        Assertions.assertFalse(lateBoundSpanProcessorInstance.isResolvable());
    }
}
