package io.quarkiverse.opentelemetry.exporter.azure.deployment;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import com.azure.core.annotation.ServiceInterface;
import com.azure.core.http.HttpClientProvider;
import com.azure.core.http.vertx.VertxProvider;

import io.netty.handler.ssl.OpenSsl;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkiverse.opentelemetry.exporter.azure.runtime.*;
import io.quarkus.arc.deployment.OpenTelemetrySdkBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.opentelemetry.deployment.exporter.otlp.ExternalOtelExporterBuildItem;
import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer;

@BuildSteps(onlyIf = AzureExporterProcessor.AzureExporterEnabled.class)
public class AzureExporterProcessor {

    public static class AzureExporterEnabled implements BooleanSupplier {
        AzureExporterBuildConfig azureExporterConfig;

        public boolean getAsBoolean() {
            return azureExporterConfig.enabled();
        }
    }

    private static final DotName SERVICE_INTERFACE_DOT_NAME = DotName.createSimple(ServiceInterface.class.getName());

    /**
     * CDI priority of the synthetic beans that customize the OpenTelemetry SDK builder.
     * <p>
     * This value is load-bearing. ArC iterates {@code Instance<AutoConfiguredOpenTelemetrySdkBuilderCustomizer>}
     * in descending priority order and, for equal priorities, in hash order that changes from one JVM to the next.
     * The Quarkus built-in customizers have priority 0 and the Quarkus {@code MetricProviderCustomizer} rebuilds the
     * periodic metric reader from the metric exporter it observed in the customizer chain. When it is iterated before
     * the Azure customizer, that exporter is the Azure Monitor no-op marker and every metric is silently dropped.
     * A higher priority guarantees that the Azure customizations are registered first.
     * {@link AzureMonitorCustomizer} additionally re-binds the reader to the Azure exporter whatever the order.
     */
    public static final int SDK_CUSTOMIZER_PRIORITY = 100;

    @BuildStep
    IndexDependencyBuildItem indexDependency() {
        return new IndexDependencyBuildItem("com.azure", "azure-core");
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(OpenSsl.class.getName()));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem("io.netty.internal.tcnative.SSL"));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem("io.netty.util.concurrent.GlobalEventExecutor"));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(
                "com.azure.core.http.vertx.VertxHttpClientProvider$GlobalVertxHttpClient"));
        runtimeInitializedClasses.produce(
                new RuntimeInitializedClassBuildItem("com.azure.core.http.vertx.VertxHttpClientBuilder$DefaultVertx"));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(
                "com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.QuickPulseDataCollector"));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(
                "com.azure.monitor.opentelemetry.autoconfigure.implementation.heartbeat.HeartbeatDefaultPayload"));
    }

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                com.azure.core.util.DateTimeRfc1123.class,
                com.azure.core.http.rest.StreamResponse.class,
                com.azure.core.http.rest.ResponseBase.class,
                com.azure.core.http.HttpHeaderName.class).build());

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "com.microsoft.aad.msal4j.AadInstanceDiscoveryResponse",
                "com.microsoft.aad.msal4j.InstanceDiscoveryMetadataEntry").fields().build());

    }

    @BuildStep
    void nativeResources(BuildProducer<ServiceProviderBuildItem> services,
            BuildProducer<NativeImageResourceBuildItem> nativeResources) {
        Stream.of(
                HttpClientProvider.class.getName(), // TODO move this to a separate camel-quarkus-azure-core extension
                "reactor.blockhound.integration.BlockHoundIntegration" // TODO: move to reactor extension

        )
                .forEach(service -> {
                    try {
                        Set<String> implementations = ServiceUtil.classNamesNamedIn(
                                Thread.currentThread().getContextClassLoader(),
                                "META-INF/services/" + service);
                        services.produce(
                                new ServiceProviderBuildItem(service,
                                        implementations.toArray(new String[0])));

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        nativeResources.produce(new NativeImageResourceBuildItem(
                "azure-core.properties"));
    }

    @BuildStep
    void proxyDefinitions(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinitions) {

        combinedIndex
                .getIndex()
                .getAnnotations(SERVICE_INTERFACE_DOT_NAME)
                .stream()
                .map(annotationInstance -> annotationInstance.target().asClass().name().toString())
                .map(NativeImageProxyDefinitionBuildItem::new)
                .forEach(proxyDefinitions::produce);
    }

    @BuildStep
    void registerServiceProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(ServiceProviderBuildItem.allProvidersFromClassPath(VertxProvider.class.getName()));
    }

    @BuildStep
    void registerExternalExporter(BuildProducer<ExternalOtelExporterBuildItem> buildProducer) {
        buildProducer.produce(new ExternalOtelExporterBuildItem("azure"));
    }

    @BuildStep
    @Consume(OpenTelemetrySdkBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem openTelemetryCustomizer(AzureRecorder recorder) {
        return SyntheticBeanBuildItem.configure(AutoConfiguredOpenTelemetrySdkBuilderCustomizer.class)
                .types(AutoConfigurationCustomizerProvider.class)
                .setRuntimeInit()
                .scope(Singleton.class)
                .unremovable()
                .priority(SDK_CUSTOMIZER_PRIORITY)
                // the Quarkus managed scheduler, used for the periodic metric reader like Quarkus does
                .addInjectionPoint(ParameterizedType.create(DotName.createSimple(Instance.class),
                        new Type[] { ClassType.create(DotName.createSimple(ScheduledExecutorService.class.getName())) },
                        null))
                .createWith(recorder.createAzureMonitorCustomizer())
                .done();

    }

    /**
     * Quarkus uses the {@link Sampler} bean with the highest priority. {@link #SDK_CUSTOMIZER_PRIORITY} makes the
     * Azure endpoint sampler take precedence over an application sampler with the default priority; the application
     * can still override it with a higher priority (see the "Sampling" section of the extension documentation).
     */
    @BuildStep
    @Consume(OpenTelemetrySdkBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem installAzureEndpointSampler(AzureRecorder recorder) {
        return SyntheticBeanBuildItem.configure(AzureEndpointSampler.class)
                .types(Sampler.class)
                .setRuntimeInit()
                .scope(Singleton.class)
                .unremovable()
                .priority(SDK_CUSTOMIZER_PRIORITY)
                .createWith(recorder.createSampler())
                .done();
    }
}
