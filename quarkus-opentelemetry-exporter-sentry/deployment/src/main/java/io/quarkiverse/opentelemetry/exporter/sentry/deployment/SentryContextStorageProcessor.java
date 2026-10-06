package io.quarkiverse.opentelemetry.exporter.sentry.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;

/**
 * Both Quarkus OpenTelemetry and Sentry register an {@code io.opentelemetry.context.ContextStorageProvider} via the
 * ServiceLoader. When more than one is present OpenTelemetry cannot pick one, logs a warning and silently falls back to the
 * plain thread-local {@code ContextStorage}, losing Quarkus' Vert.x-aware propagation and Sentry's scope synchronization.
 * <p>
 * Sentry's provider is designed to wrap the other one, so we select it explicitly: it delegates to Quarkus'
 * {@code ContextStorage} while keeping Sentry scopes in sync. This must be done regardless of whether the exporter is
 * enabled, because the {@code sentry-opentelemetry-bootstrap} jar (and therefore the provider) is always on the classpath
 * when this extension is present.
 */
public final class SentryContextStorageProcessor {

    private static final String OTEL_CONTEXT_STORAGE_PROVIDER_PROPERTY = "io.opentelemetry.context.contextStorageProvider";
    private static final String SENTRY_CONTEXT_STORAGE_PROVIDER = "io.sentry.opentelemetry.SentryContextStorageProvider";

    @BuildStep
    SystemPropertyBuildItem selectSentryContextStorageProvider() {
        return new SystemPropertyBuildItem(OTEL_CONTEXT_STORAGE_PROVIDER_PROPERTY, SENTRY_CONTEXT_STORAGE_PROVIDER);
    }
}
