package io.quarkiverse.opentelemetry.exporter.gcp.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

import java.util.Optional;

public class GcpExporterConfig {
    @ConfigRoot(name = "opentelemetry.tracer.exporter.gcp", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
    public static class GcpExporterBuildConfig {
        /**
         * GCP Opentelemetry trace exporter support.
         * <p>
         * GCP Opentelemetry support is enabled by default.
         */
        @ConfigItem(defaultValue = "true")
        public Boolean enabled;

        /**
         * Support for Cloud Run environments. Cloud Run doesn't support background processes so we need to use different
         * SpanProcessor.
         * <a href="https://cloud.google.com/trace/docs/setup/java-ot#export">Reference</a>
         */
        @ConfigItem(defaultValue = "false")
        public Boolean cloudrun;
    }

    @ConfigRoot(name = "opentelemetry.tracer.exporter.gcp", phase = ConfigPhase.RUN_TIME)
    public static class GcpExporterRuntimeConfig {
        /**
         * Override for GCP Project ID.
         */
        @ConfigItem
        @ConvertWith(TrimmedStringConverter.class)
        public Optional<String> projectid;
    }
}
