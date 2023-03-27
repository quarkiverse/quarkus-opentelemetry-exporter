package io.quarkiverse.opentelemetry.exporter.gcp.runtime;

import java.io.IOException;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.CDI;

import com.google.cloud.opentelemetry.trace.TestTraceConfigurationBuilder;
import com.google.cloud.opentelemetry.trace.TraceConfiguration;
import com.google.cloud.opentelemetry.trace.TraceExporter;

import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.quarkiverse.opentelemetry.exporter.common.runtime.LateBoundSpanProcessor;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class GcpRecorder {
    public void installSpanProcessorForGcp(GcpExporterConfig.GcpExporterRuntimeConfig runtimeConfig, LaunchMode launchMode) {
        if (launchMode != LaunchMode.TEST && runtimeConfig.endpoint.isEmpty()) {
            try {
                configureTraceExporter(runtimeConfig);
            } catch (IOException e) {
                throw new RuntimeException("Unable to initialize GCP TraceExporter.", e);
            }
        } else {
            TraceConfiguration.Builder builder = TestTraceConfigurationBuilder.buildTestTraceConfiguration();

            if (runtimeConfig.endpoint.isPresent() && runtimeConfig.endpoint.get().trim().length() > 0) {
                builder.setTraceServiceEndpoint(runtimeConfig.endpoint.get());
            }

            TraceConfiguration config = builder.build();
            try {
                if (runtimeConfig.cloudrun) {
                    configureSimpleSpanExporter(config);
                } else {
                    configureBatchSpanExporter(config);
                }
            } catch (IOException e) {
                throw new RuntimeException("Unable to initialize GCP TraceExporter.", e);
            }
        }

    }

    private void configureTraceExporter(GcpExporterConfig.GcpExporterRuntimeConfig runtimeConfig) throws IOException {
        TraceConfiguration.Builder builder = TraceConfiguration.builder();

        if (runtimeConfig.projectid.isPresent() && runtimeConfig.projectid.get().trim().length() > 0) {
            builder.setProjectId(runtimeConfig.projectid.get());
        }

        TraceConfiguration traceConfig = builder.build();
        // Initialize GCP TraceExporter default configuration
        if (runtimeConfig.cloudrun) {
            configureSimpleSpanExporter(traceConfig);
        } else {
            configureBatchSpanExporter(traceConfig);
        }
    }

    private void configureBatchSpanExporter(TraceConfiguration config) throws IOException {
        BatchSpanProcessor batchSpanProcessor = BatchSpanProcessor.builder(TraceExporter.createWithConfiguration(config))
                .build();

        LateBoundSpanProcessor delayedProcessor = CDI.current()
                .select(LateBoundSpanProcessor.class, Any.Literal.INSTANCE).get();

        delayedProcessor.setSpanProcessorDelegate(batchSpanProcessor);
    }

    private void configureSimpleSpanExporter(TraceConfiguration config) throws IOException {
        TraceExporter traceExporter = TraceExporter.createWithConfiguration(config);
        SpanProcessor spanProcessor = SimpleSpanProcessor.create(traceExporter);

        LateBoundSpanProcessor delayedProcessor = CDI.current()
                .select(LateBoundSpanProcessor.class, Any.Literal.INSTANCE).get();

        delayedProcessor.setSpanProcessorDelegate(spanProcessor);
    }
}
