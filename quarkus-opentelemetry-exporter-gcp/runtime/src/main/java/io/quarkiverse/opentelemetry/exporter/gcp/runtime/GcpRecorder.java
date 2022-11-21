package io.quarkiverse.opentelemetry.exporter.gcp.runtime;

import com.google.cloud.opentelemetry.trace.TraceConfiguration;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.quarkus.runtime.annotations.Recorder;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.CDI;
import java.io.IOException;

@Recorder
public class GcpRecorder {
    public void installSpanProcessorForGcp(GcpExporterConfig.GcpExporterRuntimeConfig runtimeConfig) {
        TraceConfiguration.Builder builder = TraceConfiguration.builder();

        if (runtimeConfig.projectid.isPresent() && runtimeConfig.projectid.get().trim().length() > 0) {
            builder.setProjectId(runtimeConfig.projectid.get());
        }

        // Initialize GCP TraceExporter default configuration
        try (TraceExporter traceExporter = TraceExporter.createWithConfiguration(builder.build())) {
            if (runtimeConfig.cloudrun) {
                configureSimpleSpanExporter(traceExporter);
            } else {
                configureBatchSpanExporter(traceExporter);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize GCP TraceExporter.", e);
        }
    }

    private void configureBatchSpanExporter(TraceExporter traceExporter) {
        BatchSpanProcessor batchSpanProcessor = BatchSpanProcessor.builder(traceExporter).build();

        LateBoundBatchSpanProcessor delayedProcessor = CDI.current()
                .select(LateBoundBatchSpanProcessor.class, Any.Literal.INSTANCE).get();

        delayedProcessor.setBatchSpanProcessorDelegate(batchSpanProcessor);
    }

    private void configureSimpleSpanExporter(TraceExporter traceExporter) {
        SimpleSpanProcessor spanProcessor = (SimpleSpanProcessor) SimpleSpanProcessor.create(traceExporter);

        LateBoundSimpleSpanProcessor delayedProcessor = CDI.current()
                .select(LateBoundSimpleSpanProcessor.class, Any.Literal.INSTANCE).get();

        delayedProcessor.setSimpleSpanProcessorDelegate(spanProcessor);
    }
}
