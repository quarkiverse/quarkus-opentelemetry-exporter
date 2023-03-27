package io.quarkiverse.opentelemetry.exporter.gcp.deployment;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkiverse.opentelemetry.exporter.gcp.runtime.LateBoundBatchSpanProcessor;
import io.quarkiverse.opentelemetry.exporter.gcp.runtime.LateBoundSimpleSpanProcessor;
import io.quarkus.test.QuarkusUnitTest;

public class GcpExporterEnabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.opentelemetry.tracer.exporter.gcp.enabled", "true")
            .overrideConfigKey("quarkus.opentelemetry.tracer.exporter.gcp.projectid", "test");

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    Instance<LateBoundBatchSpanProcessor> lateBoundBatchSpanProcessorInstance;

    @Inject
    Instance<LateBoundSimpleSpanProcessor> lateBoundSimpleSpanProcessors;

    @Test
    void testOpenTelemetryButNoBatchSpanProcessor() {
        Assertions.assertNotNull(openTelemetry);
        Assertions.assertTrue(lateBoundBatchSpanProcessorInstance.isResolvable());
        Assertions.assertTrue(lateBoundSimpleSpanProcessors.isResolvable());
    }
}
