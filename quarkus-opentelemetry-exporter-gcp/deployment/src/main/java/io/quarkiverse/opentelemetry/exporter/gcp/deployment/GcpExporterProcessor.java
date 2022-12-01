package io.quarkiverse.opentelemetry.exporter.gcp.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.function.BooleanSupplier;

import io.quarkiverse.opentelemetry.exporter.gcp.runtime.GcpExporterConfig;
import io.quarkiverse.opentelemetry.exporter.gcp.runtime.GcpExporterProvider;
import io.quarkiverse.opentelemetry.exporter.gcp.runtime.GcpRecorder;
import io.quarkiverse.opentelemetry.exporter.gcp.runtime.GcpSimpleSpanExporterProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;

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
    @Record(RUNTIME_INIT)
    void installBatchSpanProcessorForGcp(GcpRecorder recorder,
            LaunchModeBuildItem launchModeBuildItem,
            GcpExporterConfig.GcpExporterRuntimeConfig runtimeConfig) {
        recorder.installSpanProcessorForGcp(runtimeConfig, launchModeBuildItem.getLaunchMode());
    }
}
