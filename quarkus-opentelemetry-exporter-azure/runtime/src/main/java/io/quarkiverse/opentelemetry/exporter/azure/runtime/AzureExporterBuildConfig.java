package io.quarkiverse.opentelemetry.exporter.azure.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "opentelemetry.tracer.exporter.azure")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface AzureExporterBuildConfig {
    /**
     * Azure Span Exporter support.
     * <p>
     * Azure Span Exporter support is enabled by default.
     */
    @WithDefault("true")
    Boolean enabled();
}
