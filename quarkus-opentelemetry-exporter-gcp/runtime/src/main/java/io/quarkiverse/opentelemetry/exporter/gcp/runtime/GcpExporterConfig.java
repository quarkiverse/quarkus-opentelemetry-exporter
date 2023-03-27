package io.quarkiverse.opentelemetry.exporter.gcp.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

public class GcpExporterConfig {
    @ConfigRoot(name = "opentelemetry.tracer.exporter.gcp", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
    public static class GcpExporterBuildConfig {
        /**
         * GCP's Tracing exporter support. Enabled by default.
         */
        @ConfigItem(defaultValue = "true")
        public Boolean enabled;
    }

    @ConfigRoot(name = "opentelemetry.tracer.exporter.gcp", phase = ConfigPhase.RUN_TIME)
    public static class GcpExporterRuntimeConfig {
        /**
         * Override to set GCP's projectid.
         */
        @ConfigItem
        @ConvertWith(TrimmedStringConverter.class)
        public Optional<String> projectid;

        /**
         * Override for GCP TraceEndpoint setting.
         */
        @ConfigItem
        @ConvertWith(TrimmedStringConverter.class)
        public Optional<String> endpoint;

        /**
         * Support for Cloud Run environments. Set to `true` for Cloud Run deployments.
         * <p>
         * Cloud Run <a href="https://cloud.google.com/trace/docs/setup/java-ot#export">doesn't support background processes</a>
         * and `SimpleSpanProcessor` must be used.
         */
        @ConfigItem(defaultValue = "false")
        public Boolean cloudrun;
    }
}
