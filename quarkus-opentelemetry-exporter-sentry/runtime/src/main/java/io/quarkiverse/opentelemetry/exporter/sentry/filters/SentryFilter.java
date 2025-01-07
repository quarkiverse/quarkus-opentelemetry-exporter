package io.quarkiverse.opentelemetry.exporter.sentry.filters;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

import io.sentry.Sentry;

@SuppressWarnings("unused")
@Provider
public final class SentryFilter implements ContainerRequestFilter, DynamicFeature {
    @Override
    public void filter(final ContainerRequestContext context) {
        final var method = context.getMethod();
        final var uriInfo = context.getUriInfo();
        final var sentryRequest = new io.sentry.protocol.Request();
        sentryRequest.setApiTarget("rest");
        sentryRequest.setMethod(method);
        sentryRequest.setUrl(uriInfo.getRequestUri().toString());
        sentryRequest.setQueryString(
                uriInfo.getQueryParameters().entrySet().stream()
                        .map(entry -> entry.getKey() + '=' + String.join(",", entry.getValue()))
                        .collect(Collectors.joining("&")));
        sentryRequest.setHeaders(
                context.getHeaders().entrySet().stream()
                        .collect(
                                Collectors.toMap(Map.Entry::getKey, entry -> String.join(";", entry.getValue()))));
        sentryRequest.setCookies(
                context.getCookies().entrySet().stream()
                        .map(entry -> entry.getKey() + '=' + entry.getValue().getValue())
                        .collect(Collectors.joining(";")));

        Sentry.configureScope(
                scope -> {
                    scope.setTransaction(method + ' ' + uriInfo.getPath());
                    scope.setRequest(sentryRequest);
                });
    }

    @Override
    public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
        context.register(this);
    }
}
