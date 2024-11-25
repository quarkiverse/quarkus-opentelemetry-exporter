package io.quarkiverse.opentelemetry.exporter.sentry.recorders;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkiverse.opentelemetry.exporter.sentry.config.SentryConfig.SentryExporterRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.sentry.Instrumenter;
import io.sentry.Sentry;
import io.sentry.SentryOptions;
import io.sentry.jul.SentryHandler;
import io.sentry.opentelemetry.OpenTelemetryLinkErrorEventProcessor;

@Recorder
public class SentryRecorder {
    public RuntimeValue<Optional<Handler>> create(final SentryExporterRuntimeConfig sentryConfig) {
        if (sentryConfig.dsn().isEmpty()) {
            return new RuntimeValue<>(Optional.empty());
        }

        final var config = ConfigProvider.getConfig();
        final var appName = config.getValue("quarkus.application.name", String.class);
        final var appVersion = config.getValue("quarkus.application.version", String.class);
        final var options = new AtomicReference<SentryOptions>();

        Sentry.init(
                it -> {

                    sentryConfig.dsn().ifPresent(it::setDsn);
                    sentryConfig.environment().ifPresent(it::setEnvironment);
                    sentryConfig.tracesSampleRate().ifPresent(it::setTracesSampleRate);
                    sentryConfig.inAppPackages().ifPresent(packages -> packages.forEach(it::addInAppInclude));
                    sentryConfig.debug().ifPresent(it::setDebug);
                    sentryConfig.spotlightConnectionUrl().ifPresent(url -> {
                        it.setSpotlightConnectionUrl(url);
                        it.setEnableSpotlight(true);
                    });
                    it.setRelease(appName + '@' + appVersion);
                    it.setInstrumenter(Instrumenter.OTEL);
                    it.addEventProcessor(new OpenTelemetryLinkErrorEventProcessor());
                    options.set(it);
                });

        final var handler = new SentryHandler(options.get());
        handler.setPrintfStyle(true);
        handler.setLevel(Level.WARNING);
        handler.setMinimumEventLevel(Level.WARNING);
        handler.setMinimumBreadcrumbLevel(Level.INFO);
        return new RuntimeValue<>(Optional.of(handler));
    }
}
