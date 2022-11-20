package io.quarkiverse.opentelemetry.exporter.gcp.deployment;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkiverse.opentelemetry.exporter.gcp.runtime.LateBoundBatchSpanProcessor;
import io.quarkiverse.opentelemetry.exporter.gcp.runtime.LateBoundSimpleSpanProcessor;
import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

public class GcpExporterDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.opentelemetry.tracer.exporter.gcp.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    Instance<LateBoundBatchSpanProcessor> lateBoundBatchSpanProcessorInstance;

    @Inject
    Instance<LateBoundSimpleSpanProcessor> lateBoundSimpleSpanProcessors;

    @Test
    void testOpenTelemetryButNoBatchSpanProcessor() {
        Assertions.assertNotNull(openTelemetry);
        Assertions.assertFalse(lateBoundBatchSpanProcessorInstance.isResolvable());
        Assertions.assertFalse(lateBoundSimpleSpanProcessors.isResolvable());
    }
}
