package io.quarkiverse.opentelemetry.exporter.sentry.deployment;

import static io.quarkus.deployment.Capability.REST;
import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.function.BooleanSupplier;

import io.quarkiverse.opentelemetry.exporter.sentry.beans.SentrySpanProcessorProducer;
import io.quarkiverse.opentelemetry.exporter.sentry.config.SentryConfig;
import io.quarkiverse.opentelemetry.exporter.sentry.config.SentryConfig.SentryExporterRuntimeConfig;
import io.quarkiverse.opentelemetry.exporter.sentry.filters.SentryFilter;
import io.quarkiverse.opentelemetry.exporter.sentry.recorders.SentryRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.opentelemetry.deployment.exporter.otlp.ExternalOtelExporterBuildItem;

@BuildSteps(onlyIf = SentryProcessor.SentryExporterEnabled.class)
public final class SentryProcessor {

    static class SentryExporterEnabled implements BooleanSupplier {
        SentryConfig.SentryExporterBuildConfig sentryExporterConfig;

        public boolean getAsBoolean() {
            return sentryExporterConfig.enabled();
        }
    }

    private static final String FEATURE = "sentry";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerExternalExporter(BuildProducer<ExternalOtelExporterBuildItem> buildProducer) {
        buildProducer.produce(new ExternalOtelExporterBuildItem("sentry"));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    LogHandlerBuildItem addSentryHandler(final SentryExporterRuntimeConfig config, final SentryRecorder recorder) {
        return new LogHandlerBuildItem(recorder.create(config));
    }

    @BuildStep
    void additionalBeanSentryFilter(Capabilities capabilities, BuildProducer<AdditionalBeanBuildItem> producer) {
        if (!capabilities.isPresent(REST)) {
            return;
        }
        producer.produce(AdditionalBeanBuildItem.builder().addBeanClass(SentryFilter.class).build());
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeanProducers() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(SentrySpanProcessorProducer.class).build();
    }
}
