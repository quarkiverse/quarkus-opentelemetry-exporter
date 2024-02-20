package io.quarkiverse.opentelemetry.exporter.azure.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

public class AzureExporterConfig {
    @ConfigMapping(prefix = "opentelemetry.tracer.exporter.azure")
    public static class AzureExporterBuildConfig {
        /**
         * Azure Span Exporter support.
         * <p>
         * Azure Span Exporter support is enabled by default.
         */
        @WithDefault("true")
        public Boolean enabled;
    }

    @ConfigMapping(prefix = "quarkus.otel.azure")
    @ConfigRoot(phase = ConfigPhase.RUN_TIME)
    public static class AzureExporterRuntimeConfig {
        /**
         * The Azure connection string.
         */
        @WithConverter(TrimmedStringConverter.class)
        @WithName("applicationinsights.connection.string")
        public String connectionString;
    }
}
