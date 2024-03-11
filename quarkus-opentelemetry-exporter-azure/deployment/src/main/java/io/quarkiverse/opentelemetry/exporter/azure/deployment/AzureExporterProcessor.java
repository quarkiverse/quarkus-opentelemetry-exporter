package io.quarkiverse.opentelemetry.exporter.azure.deployment;

import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

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
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.opentelemetry.deployment.exporter.otlp.ExternalOtelExporterBuildItem;

@BuildSteps(onlyIf = AzureExporterProcessor.AzureExporterEnabled.class)
public class AzureExporterProcessor {

    public static class AzureExporterEnabled implements BooleanSupplier {
        AzureExporterBuildConfig azureExporterConfig;

        public boolean getAsBoolean() {
            return azureExporterConfig.enabled();
        }
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        Stream.of(
                /*
                 * The following io.netty.util.* items were not accepted
                 * to quarkus via https://github.com/quarkusio/quarkus/pull/14994
                 * Keeping them here for now
                 */
                "reactor.netty.http.client.HttpClientSecure",
                "reactor.netty.tcp.TcpClientSecure")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClasses::produce);
    }

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "reactor.netty.channel.BootstrapHandlers$BootstrapInitializerHandler",
                "reactor.netty.channel.ChannelOperationsHandler",
                "reactor.netty.resources.PooledConnectionProvider$PooledConnectionAllocator$PooledConnectionInitializer",
                "reactor.netty.tcp.SslProvider$SslReadHandler").methods().build());

    }

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
