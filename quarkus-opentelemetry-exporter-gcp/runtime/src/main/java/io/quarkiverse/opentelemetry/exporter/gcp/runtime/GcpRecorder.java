package io.quarkiverse.opentelemetry.exporter.gcp.runtime;

import java.io.IOException;
import java.util.function.Function;

import com.google.cloud.opentelemetry.trace.TestTraceConfigurationBuilder;
import com.google.cloud.opentelemetry.trace.TraceConfiguration;
import com.google.cloud.opentelemetry.trace.TraceExporter;

import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.quarkiverse.opentelemetry.exporter.common.runtime.LateBoundSpanProcessor;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class GcpRecorder {

    private final RuntimeValue<GcpExporterConfig.GcpExporterRuntimeConfig> runtimeConfig;

    public GcpRecorder(RuntimeValue<GcpExporterConfig.GcpExporterRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public Function<SyntheticCreationalContext<LateBoundSpanProcessor>, LateBoundSpanProcessor> installSpanProcessorForGcp(
            LaunchMode launchMode) {
        return new Function<>() {
            @Override
            public LateBoundSpanProcessor apply(
                    SyntheticCreationalContext<LateBoundSpanProcessor> lateBoundSpanProcessorSyntheticCreationalContext) {
                var config = runtimeConfig.getValue();
                if (launchMode != LaunchMode.TEST && config.endpoint().isEmpty()) {
                    try {
                        return configureTraceExporter(config);
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to initialize GCP TraceExporter.", e);
                    }
                } else {
                    TraceConfiguration.Builder builder = TestTraceConfigurationBuilder.buildTestTraceConfiguration();

                    if (config.endpoint().isPresent() && config.endpoint().get().trim().length() > 0) {
                        builder.setTraceServiceEndpoint(config.endpoint().get());
                    }

                    TraceConfiguration traceConfig = builder.build();
                    try {
                        if (config.cloudrun()) {
                            return configureSimpleSpanExporter(traceConfig);
                        } else {
                            return configureBatchSpanExporter(traceConfig);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to initialize GCP TraceExporter.", e);
                    }
                }
            }
        };
    }

    private LateBoundSpanProcessor configureTraceExporter(GcpExporterConfig.GcpExporterRuntimeConfig runtimeConfig)
            throws IOException {
        TraceConfiguration.Builder builder = TraceConfiguration.builder();

        if (runtimeConfig.projectid().isPresent() && runtimeConfig.projectid().get().trim().length() > 0) {
            builder.setProjectId(runtimeConfig.projectid().get());
        }

        TraceConfiguration traceConfig = builder.build();
        // Initialize GCP TraceExporter default configuration
        if (runtimeConfig.cloudrun()) {
            return configureSimpleSpanExporter(traceConfig);
        } else {
            return configureBatchSpanExporter(traceConfig);
        }
    }

    private LateBoundSpanProcessor configureBatchSpanExporter(TraceConfiguration config) throws IOException {
        BatchSpanProcessor batchSpanProcessor = BatchSpanProcessor
                .builder(TraceExporter.createWithConfiguration(config))
                .build();
        return new LateBoundSpanProcessor(batchSpanProcessor);
    }

    private LateBoundSpanProcessor configureSimpleSpanExporter(TraceConfiguration config) throws IOException {
        SpanProcessor spanProcessor = SimpleSpanProcessor.create(TraceExporter.createWithConfiguration(config));
        return new LateBoundSpanProcessor(spanProcessor);
    }
}
