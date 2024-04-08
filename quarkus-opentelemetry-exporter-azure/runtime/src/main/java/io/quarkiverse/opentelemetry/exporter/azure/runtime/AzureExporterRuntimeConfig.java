package io.quarkiverse.opentelemetry.exporter.azure.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "applicationinsights")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface AzureExporterRuntimeConfig {
    /**
     * The Azure connection string. Same as `quarkus.otel.azure.applicationinsights.connection.string` but with higher priority. Created for convenience because it's the recommended property name in Azure cloud.
     */
    @WithConverter(TrimmedStringConverter.class)
    @WithName("connection.string")
    Optional<String> connectionString();
}
