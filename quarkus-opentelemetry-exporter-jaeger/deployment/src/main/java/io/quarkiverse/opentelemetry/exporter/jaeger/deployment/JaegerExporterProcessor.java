package io.quarkiverse.opentelemetry.exporter.jaeger.deployment;

import java.util.function.BooleanSupplier;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.opentelemetry.exporter.internal.grpc.GrpcSenderProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkiverse.opentelemetry.exporter.common.runtime.LateBoundSpanProcessor;
import io.quarkiverse.opentelemetry.exporter.jaeger.runtime.JaegerExporterConfig;
import io.quarkiverse.opentelemetry.exporter.jaeger.runtime.JaegerRecorder;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.opentelemetry.deployment.exporter.otlp.ExternalOtelExporterBuildItem;

@BuildSteps(onlyIf = JaegerExporterProcessor.JaegerExporterEnabled.class)
public class JaegerExporterProcessor {

    static class JaegerExporterEnabled implements BooleanSupplier {
        JaegerExporterConfig.JaegerExporterBuildConfig jaegerExporterConfig;

        public boolean getAsBoolean() {
            return jaegerExporterConfig.enabled;
        }
    }

    @BuildStep
    void retainBeans(BuildProducer<ServiceProviderBuildItem> services) {

        services.produce(new ServiceProviderBuildItem(GrpcSenderProvider.class.getName(),
                "io.opentelemetry.exporter.sender.okhttp.internal.OkHttpGrpcSenderProvider"));
    }

    @BuildStep
    void registerExternalExporter(BuildProducer<ExternalOtelExporterBuildItem> buildProducer) {
        buildProducer.produce(new ExternalOtelExporterBuildItem("jaeger"));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem installBatchSpanProcessorForJaeger(JaegerRecorder recorder,
            LaunchModeBuildItem launchModeBuildItem,
            JaegerExporterConfig.JaegerExporterRuntimeConfig runtimeConfig) {
        return SyntheticBeanBuildItem.configure(LateBoundSpanProcessor.class)
                .types(SpanProcessor.class)
                .setRuntimeInit()
                .scope(Singleton.class)
                .unremovable()
                .addInjectionPoint(ParameterizedType.create(DotName.createSimple(Instance.class),
                        new Type[] { ClassType.create(DotName.createSimple(SpanExporter.class.getName())) }, null))
                .createWith(recorder.createBatchSpanProcessorForJaeger(runtimeConfig))
                .done();
    }
}
