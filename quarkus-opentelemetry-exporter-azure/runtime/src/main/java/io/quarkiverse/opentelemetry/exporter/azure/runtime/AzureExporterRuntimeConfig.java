package io.quarkiverse.opentelemetry.exporter.azure.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.otel.azure")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface AzureExporterRuntimeConfig {
    /**
     * The Azure connection string.
     */
    @WithConverter(TrimmedStringConverter.class)
    @WithName("applicationinsights.connection.string")
    public String connectionString();
}