package io.quarkiverse.opentelemetry.exporter.azure.deployment;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkiverse.opentelemetry.exporter.azure.runtime.AzureEndpointSampler;
import io.quarkus.test.QuarkusUnitTest;

public class AzureExporterEnabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.azure.enabled", "true")
            .overrideConfigKey("applicationinsights.connection.string",
                    "InstrumentationKey=bla;IngestionEndpoint=http://127.0.0.1:53602/export");

    @Inject
    OpenTelemetry openTelemetry;

    // just some class that will be available if the Azure exporter is enabled.
    @Inject
    Instance<AzureEndpointSampler> azureEndpointSamplers;

    @Test
    void testOpenTelemetryButNoBatchSpanProcessor() {
        Assertions.assertNotNull(openTelemetry);
        Assertions.assertTrue(azureEndpointSamplers.isResolvable());
        Assertions.assertEquals(1, azureEndpointSamplers.stream().count());
        Assertions.assertEquals(AzureEndpointSampler.class.getName(), azureEndpointSamplers.get().getClass().getName());
    }
}
