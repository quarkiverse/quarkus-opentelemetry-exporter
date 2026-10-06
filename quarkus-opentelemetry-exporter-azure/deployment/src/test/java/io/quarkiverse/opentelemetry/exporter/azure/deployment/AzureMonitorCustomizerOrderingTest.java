package io.quarkiverse.opentelemetry.exporter.azure.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.azure.monitor.opentelemetry.autoconfigure.AzureMonitorAutoConfigureOptions;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.quarkiverse.opentelemetry.exporter.azure.runtime.AzureMonitorCustomizer;
import io.quarkus.test.QuarkusUnitTest;
import reactor.core.publisher.Mono;

/**
 * The Quarkus {@code MetricProviderCustomizer} captures the metric exporter it observes in the exporter customizer
 * chain and rebuilds the periodic metric reader from it. The order in which SDK builder customizers are iterated is
 * not deterministic, so the Azure customizer must bind the reader to the Azure Monitor exporter whether such a
 * customizer runs before or after it. This test applies the customizers by hand in both orders; the Quarkus
 * application only provides the CDI container the Azure Vert.x transport needs.
 */
class AzureMonitorCustomizerOrderingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.azure.enabled", "false");

    private static final Duration EXPORT_INTERVAL = Duration.ofHours(1);

    private static final String CONNECTION_STRING = "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=http://127.0.0.1:1/export";

    @Test
    void readerIsBoundToAzureExporterWhenReaderRebuildingCustomizerRunsFirst() {
        assertReaderBoundToAzureExporter(true);
    }

    @Test
    void readerIsBoundToAzureExporterWhenReaderRebuildingCustomizerRunsLast() {
        assertReaderBoundToAzureExporter(false);
    }

    private static void assertReaderBoundToAzureExporter(boolean quarkusLikeCustomizerFirst) {
        AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder()
                .disableShutdownHook()
                .addPropertiesSupplier(() -> Map.of("otel.metric.export.interval", "1h"));
        AzureMonitorCustomizer azureCustomizer = new AzureMonitorCustomizer(Optional.of(CONNECTION_STRING),
                EXPORT_INTERVAL, null) {
            @Override
            protected AzureMonitorAutoConfigureOptions createAutoConfigureOptions(String connectionString) {
                // no CDI container here, so the Vert.x transport cannot be created: nothing is sent in this test
                return super.createAutoConfigureOptions(connectionString)
                        .httpClient(request -> Mono.error(new UnsupportedOperationException("no transport")));
            }
        };
        if (quarkusLikeCustomizerFirst) {
            captureExporterAndRebuildReader(builder);
            azureCustomizer.customize(builder);
        } else {
            azureCustomizer.customize(builder);
            captureExporterAndRebuildReader(builder);
        }

        try (OpenTelemetrySdk sdk = builder.build().getOpenTelemetrySdk()) {
            String meterProvider = sdk.getSdkMeterProvider().toString();
            assertThat(meterProvider)
                    .contains("PeriodicMetricReader{exporter=" + AzureMonitorCustomizer.AZURE_METRIC_EXPORTER_CLASS_NAME)
                    .doesNotContain("exporter=INSTANCE"); // the Azure no-op MarkerMetricExporter enum constant
        }
    }

    /** Mimics io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer.MetricProviderCustomizer. */
    private static void captureExporterAndRebuildReader(AutoConfiguredOpenTelemetrySdkBuilder builder) {
        AtomicReference<MetricExporter> captured = new AtomicReference<>();
        builder.addMetricExporterCustomizer((exporter, config) -> {
            captured.set(exporter);
            return exporter;
        }).addMetricReaderCustomizer((reader, config) -> {
            if (reader instanceof PeriodicMetricReader) {
                return PeriodicMetricReader.builder(captured.get()).setInterval(EXPORT_INTERVAL).build();
            }
            return reader;
        });
    }
}
