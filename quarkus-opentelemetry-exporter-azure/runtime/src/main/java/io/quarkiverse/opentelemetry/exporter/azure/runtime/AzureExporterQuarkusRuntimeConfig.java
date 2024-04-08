package io.quarkiverse.opentelemetry.exporter.azure.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.otel.azure")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface AzureExporterQuarkusRuntimeConfig {
    /**
     * The Azure connection string. Same as `applicationinsights.connection.string`. Setting `applicationinsights.connection.string` takes precedence over the value set here.
     */
    @WithConverter(TrimmedStringConverter.class)
    @WithName("applicationinsights.connection.string")
    Optional<String> connectionString();
}
