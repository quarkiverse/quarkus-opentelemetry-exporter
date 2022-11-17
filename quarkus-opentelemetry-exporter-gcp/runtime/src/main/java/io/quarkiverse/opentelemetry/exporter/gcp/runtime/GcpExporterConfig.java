package io.quarkiverse.opentelemetry.exporter.gcp.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

public class GcpExporterConfig {
    @ConfigRoot(name = "opentelemetry.tracer.exporter.gcp", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
    public static class GcpExporterBuildConfig {
        /**
         * GCP SpanExporter support.
         * <p>
         * GCP SpanExporter support is enabled by default.
         */
        @ConfigItem(defaultValue = "true")
        public Boolean enabled;

        /**
         * Support for Cloud Run environments. Cloud Run doesn't support background processes so we need to use different
         * SpanProcessor.
         *
         * Reference: https://cloud.google.com/trace/docs/setup/java-ot#export
         */
        @ConfigItem(defaultValue = "false")
        public Boolean cloudrun;
    }
}
