package io.quarkiverse.opentelemetry.exporter.azure.deployment;

import java.util.function.BooleanSupplier;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkiverse.opentelemetry.exporter.azure.runtime.AzureExporterConfig;
import io.quarkiverse.opentelemetry.exporter.azure.runtime.AzureRecorder;
import io.quarkiverse.opentelemetry.exporter.common.runtime.LateBoundSpanProcessor;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.*;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.opentelemetry.deployment.exporter.otlp.ExternalOtelExporterBuildItem;

@BuildSteps(onlyIf = AzureExporterProcessor.AzureExporterEnabled.class)
public class AzureExporterProcessor {

    static class AzureExporterEnabled implements BooleanSupplier {
        AzureExporterConfig.AzureExporterBuildConfig jaegerExporterConfig;

        public boolean getAsBoolean() {
            return jaegerExporterConfig.enabled;
        }
    }

    @BuildStep
    void registerExternalExporter(BuildProducer<ExternalOtelExporterBuildItem> buildProducer) {
        buildProducer.produce(new ExternalOtelExporterBuildItem("azure"));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem installBatchSpanProcessorForAzure(AzureRecorder recorder,
            AzureExporterConfig.AzureExporterRuntimeConfig runtimeConfig) {
        return SyntheticBeanBuildItem.configure(LateBoundSpanProcessor.class)
                .types(SpanProcessor.class)
                .setRuntimeInit()
                .scope(Singleton.class)
                .unremovable()
                .addInjectionPoint(ParameterizedType.create(DotName.createSimple(Instance.class),
                        new Type[] { ClassType.create(DotName.createSimple(SpanExporter.class.getName())) }, null))
                .createWith(recorder.createSpanProcessorForAzure(runtimeConfig))
                .done();
    }
}
