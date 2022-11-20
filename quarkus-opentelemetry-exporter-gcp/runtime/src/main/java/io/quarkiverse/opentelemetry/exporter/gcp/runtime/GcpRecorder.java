package io.quarkiverse.opentelemetry.exporter.gcp.runtime;

import com.google.cloud.opentelemetry.trace.TraceConfiguration;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import com.google.cloud.trace.v2.stub.TraceServiceStub;
import com.google.cloud.trace.v2.stub.TraceServiceStubSettings;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.Recorder;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.CDI;
import java.io.IOException;

@Recorder
public class GcpRecorder {
    public void installSpanProcessorForGcp(GcpExporterConfig.GcpExporterRuntimeConfig runtimeConfig, GcpExporterConfig.GcpExporterBuildConfig buildConfig, LaunchMode launchMode) {
        TraceConfiguration.Builder builder = TraceConfiguration.builder();

        if (launchMode == LaunchMode.TEST) {
            try {
                TraceServiceStubSettings build = TraceServiceStubSettings.newBuilder().build();
                TraceServiceStub stub = build.createStub();
                builder.setTraceServiceStub(stub);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (runtimeConfig.projectid.isPresent() && runtimeConfig.projectid.get().trim().length() > 0) {
            builder.setProjectId(runtimeConfig.projectid.get());
        }

        // Initialize GCP TraceExporter default configuration
        try (TraceExporter traceExporter = TraceExporter.createWithConfiguration(builder.build())) {
            if (buildConfig.cloudrun) {
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
