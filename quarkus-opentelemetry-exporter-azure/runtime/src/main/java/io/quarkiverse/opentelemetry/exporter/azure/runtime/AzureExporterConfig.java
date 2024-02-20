package io.quarkiverse.opentelemetry.exporter.azure.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

public class AzureExporterConfig {
    @ConfigRoot(name = "applicationinsights", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
    public static class AzureExporterBuildConfig {

    }

    @ConfigRoot(name = "applicationinsights.connection.string", phase = ConfigPhase.RUN_TIME)
    public static class AzureExporterRuntimeConfig {

        /**
         * The Azure connection string
         */
        @ConvertWith(TrimmedStringConverter.class)
        //applicationinsights.connection.string
        public String string;

    }
}
