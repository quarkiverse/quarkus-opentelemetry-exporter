package io.quarkiverse.opentelemetry.exporter.gcp.deployment;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.quarkiverse.opentelemetry.exporter.gcp.runtime.LateBoundBatchSpanProcessor;
import io.quarkus.test.QuarkusUnitTest;

public class GcpSimpleExporterEnabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.opentelemetry.tracer.exporter.gcp.enabled", "true")
            .overrideConfigKey("quarkus.opentelemetry.tracer.exporter.gcp.cloudrun", "true");

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    Instance<LateBoundBatchSpanProcessor> lateBoundBatchSpanProcessorInstance;

    @Inject
    Instance<SimpleSpanProcessor> simpleSpanProcessor;

    @Test
    void testOpenTelemetryButNoBatchSpanProcessor() {
        Assertions.assertNotNull(openTelemetry);
        Assertions.assertFalse(lateBoundBatchSpanProcessorInstance.isResolvable());
        Assertions.assertTrue(simpleSpanProcessor.isResolvable());
    }
}
