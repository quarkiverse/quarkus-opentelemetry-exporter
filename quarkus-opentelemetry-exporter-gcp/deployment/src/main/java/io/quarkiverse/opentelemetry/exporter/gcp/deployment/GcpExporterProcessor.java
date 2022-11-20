package io.quarkiverse.opentelemetry.exporter.gcp.deployment;

import io.quarkiverse.opentelemetry.exporter.gcp.runtime.GcpExporterConfig;
import io.quarkiverse.opentelemetry.exporter.gcp.runtime.GcpExporterProvider;
import io.quarkiverse.opentelemetry.exporter.gcp.runtime.GcpRecorder;
import io.quarkiverse.opentelemetry.exporter.gcp.runtime.GcpSimpleSpanExporterProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;

import java.util.function.BooleanSupplier;

@BuildSteps(onlyIf = GcpExporterProcessor.GcpExporterEnabled.class)
public class GcpExporterProcessor {

    static class GcpExporterEnabled implements BooleanSupplier {
        GcpExporterConfig.GcpExporterBuildConfig gcpExporterConfig;

        public boolean getAsBoolean() {
            return gcpExporterConfig.enabled;
        }
    }

    @BuildStep
    AdditionalBeanBuildItem createBatchSpanProcessor() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(GcpExporterProvider.class)
                .setUnremovable().build();
    }

    @BuildStep
    AdditionalBeanBuildItem createSimpleSpanProcessor() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(GcpSimpleSpanExporterProvider.class)
                .setUnremovable().build();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void installBatchSpanProcessorForGcp(GcpRecorder recorder,
                                         GcpExporterConfig.GcpExporterRuntimeConfig runtimeConfig,
                                         GcpExporterConfig.GcpExporterBuildConfig buildConfig) {
        recorder.installSpanProcessorForGcp(runtimeConfig, buildConfig);
    }
}
