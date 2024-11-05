package io.quarkiverse.opentelemetry.exporter.gcp.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

public class GcpExporterConfig {
    @ConfigMapping(prefix = "quarkus.opentelemetry.tracer.exporter.gcp")
    @ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
    public interface GcpExporterBuildConfig {
        /**
         * GCP's Tracing exporter support. Enabled by default.
         */
        @WithDefault(value = "true")
        public Boolean enabled();
    }

    @ConfigMapping(prefix = "quarkus.opentelemetry.tracer.exporter.gcp")
    @ConfigRoot(phase = ConfigPhase.RUN_TIME)
    public interface GcpExporterRuntimeConfig {
        /**
         * Override to set GCP's projectid.
         */
        @WithConverter(TrimmedStringConverter.class)
        public Optional<String> projectid();

        /**
         * Override for GCP TraceEndpoint setting.
         */
        @WithConverter(TrimmedStringConverter.class)
        public Optional<String> endpoint();

        /**
         * Support for Cloud Run environments. Set to `true` for Cloud Run deployments.
         * <p>
         * Cloud Run <a href="https://cloud.google.com/trace/docs/setup/java-ot#export">doesn't support background processes</a>
         * and `SimpleSpanProcessor` must be used.
         */
        @WithDefault(value = "false")
        public Boolean cloudrun();
    }
}
