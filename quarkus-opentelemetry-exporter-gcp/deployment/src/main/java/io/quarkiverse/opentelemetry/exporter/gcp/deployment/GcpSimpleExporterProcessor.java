package io.quarkiverse.opentelemetry.exporter.gcp.deployment;

import java.util.function.BooleanSupplier;

import io.quarkiverse.opentelemetry.exporter.gcp.runtime.GcpExporterConfig;
import io.quarkiverse.opentelemetry.exporter.gcp.runtime.GcpSimpleSpanExporterProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;

@BuildSteps(onlyIf = GcpSimpleExporterProcessor.GcpSimpleExporterEnabled.class)
public class GcpSimpleExporterProcessor {

    static class GcpSimpleExporterEnabled implements BooleanSupplier {
        GcpExporterConfig.GcpExporterBuildConfig gcpExporterConfig;

        public boolean getAsBoolean() {
            return gcpExporterConfig.enabled && gcpExporterConfig.cloudrun;
        }
    }

    @BuildStep
    AdditionalBeanBuildItem createSimpleSpanProcessor() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(GcpSimpleSpanExporterProvider.class)
                .setUnremovable().build();
    }
}
