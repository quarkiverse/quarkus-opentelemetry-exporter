package io.quarkiverse.opentelemetry.exporter.sentry.config;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

public class SentryConfig {

    @ConfigMapping(prefix = "quarkus.otel.sentry")
    @ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
    public interface SentryExporterBuildConfig {
        /**
         * Sentry's Tracing exporter support. Enabled by default.
         */
        @WithDefault(value = "true")
        Boolean enabled();
    }

    @ConfigMapping(prefix = "quarkus.otel.sentry")
    @ConfigRoot(phase = ConfigPhase.RUN_TIME)
    public interface SentryExporterRuntimeConfig {

        /**
         * Sentry Data Source Name.
         */
        Optional<String> dsn();

        /**
         * Environment the events are tagged with.
         */
        Optional<String> environment();

        /**
         * Percentage of performance events sent to Sentry.
         */
        @WithDefault("1.0")
        Optional<Double> tracesSampleRate();

        /**
         * Packages to flag as In App.
         */
        Optional<List<String>> inAppPackages();

        /**
         * Sentry Spotlight connection URL.
         */
        Optional<String> spotlightConnectionUrl();

        /**
         * Enable debug mode.
         */
        Optional<Boolean> debug();
    }
}
