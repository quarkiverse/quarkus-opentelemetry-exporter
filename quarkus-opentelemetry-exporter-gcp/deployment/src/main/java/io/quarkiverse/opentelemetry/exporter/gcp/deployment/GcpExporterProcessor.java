package io.quarkiverse.opentelemetry.exporter.gcp.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.function.BooleanSupplier;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkiverse.opentelemetry.exporter.common.runtime.LateBoundSpanProcessor;
import io.quarkiverse.opentelemetry.exporter.gcp.runtime.GcpExporterConfig;
import io.quarkiverse.opentelemetry.exporter.gcp.runtime.GcpRecorder;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.opentelemetry.deployment.exporter.otlp.ExternalOtelExporterBuildItem;

@BuildSteps(onlyIf = GcpExporterProcessor.GcpExporterEnabled.class)
public class GcpExporterProcessor {

    static class GcpExporterEnabled implements BooleanSupplier {
        GcpExporterConfig.GcpExporterBuildConfig gcpExporterConfig;

        public boolean getAsBoolean() {
            return gcpExporterConfig.enabled;
        }
    }

    @BuildStep
    void registerExternalExporter(BuildProducer<ExternalOtelExporterBuildItem> buildProducer) {
        buildProducer.produce(new ExternalOtelExporterBuildItem("gcp"));
    }

    @BuildStep
    NativeImageConfigBuildItem nativeImageConfiguration() {
        NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder()
                .addRuntimeReinitializedClass("com.google.protobuf.UnsafeUtil");
        return builder.build();
    }

    @BuildStep
    public void configureNativeExecutable(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder("io.grpc.netty.shaded.io.netty.channel.ProtocolNegotiators")
                        .methods()
                        .build());
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    SyntheticBeanBuildItem installBatchSpanProcessorForGcp(GcpRecorder recorder,
            LaunchModeBuildItem launchModeBuildItem,
            GcpExporterConfig.GcpExporterRuntimeConfig runtimeConfig) {

        return SyntheticBeanBuildItem.configure(LateBoundSpanProcessor.class)
                .types(SpanProcessor.class)
                .setRuntimeInit()
                .scope(Singleton.class)
                .unremovable()
                .addInjectionPoint(ParameterizedType.create(DotName.createSimple(Instance.class),
                        new Type[] { ClassType.create(DotName.createSimple(SpanExporter.class.getName())) }, null))
                .createWith(recorder.installSpanProcessorForGcp(runtimeConfig, launchModeBuildItem.getLaunchMode()))
                .done();
    }
}
