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
import io.quarkiverse.opentelemetry.exporter.azure.runtime.AzureExporterBuildConfig;
import io.quarkiverse.opentelemetry.exporter.azure.runtime.AzureExporterRuntimeConfig;
import io.quarkiverse.opentelemetry.exporter.azure.runtime.AzureRecorder;
import io.quarkiverse.opentelemetry.exporter.common.runtime.LateBoundSpanProcessor;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.opentelemetry.deployment.exporter.otlp.ExternalOtelExporterBuildItem;

@BuildSteps(onlyIf = AzureExporterProcessor.AzureExporterEnabled.class)
public class AzureExporterProcessor {

    public static class AzureExporterEnabled implements BooleanSupplier {
        AzureExporterBuildConfig azureExporterConfig;

        public boolean getAsBoolean() {
            return azureExporterConfig.enabled();
        }
    }

    //    @BuildStep
    //    NativeImageConfigBuildItem build(
    //            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
    //        NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder()
    //                .addRuntimeReinitializedClass("io.netty.handler.ssl.OpenSslClientContext");
    //        return builder.build();
    //    }

    @BuildStep
    void registerExternalExporter(BuildProducer<ExternalOtelExporterBuildItem> buildProducer) {
        buildProducer.produce(new ExternalOtelExporterBuildItem("azure"));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem installBatchSpanProcessorForAzure(AzureRecorder recorder,
            AzureExporterRuntimeConfig runtimeConfig) {
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
