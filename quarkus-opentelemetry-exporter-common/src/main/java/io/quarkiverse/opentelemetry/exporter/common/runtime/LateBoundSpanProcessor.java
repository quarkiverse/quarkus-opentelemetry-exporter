package io.quarkiverse.opentelemetry.exporter.common.runtime;

import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

public class LateBoundSpanProcessor implements SpanProcessor {
    private static final Logger log = Logger.getLogger(LateBoundSpanProcessor.class);

    private boolean warningLogged = false;
    private SpanProcessor delegate;

    public LateBoundSpanProcessor(SpanProcessor delegate) {
        this.delegate = delegate;
    }

    /**
     * If we haven't previously logged an error,
     * log an error about a missing {@code delegate} and set {@code warningLogged=true}
     */
    private <T> T handleDelegate(
            Function<SpanProcessor, T> fn,
            Supplier<T> defaultResult) {
        if (delegate == null) {
            if (!warningLogged) {
                log.warn("No SpanProcessor delegate specified, no action taken.");
                warningLogged = true;
            }
            return defaultResult.get();
        }
        return fn.apply(delegate);
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        handleDelegate(
                d -> {
                    d.onStart(parentContext, span);
                    return null;
                }, () -> null);
    }

    @Override
    public boolean isStartRequired() {
        return handleDelegate(SpanProcessor::isStartRequired, () -> false);
    }

    @Override
    public void onEnd(ReadableSpan span) {
        handleDelegate(d -> {
            delegate.onEnd(span);
            return null;
        }, () -> null);
    }

    @Override
    public boolean isEndRequired() {
        return handleDelegate(SpanProcessor::isEndRequired, () -> true);
    }

    @Override
    public CompletableResultCode shutdown() {
        return handleDelegate(SpanProcessor::shutdown, CompletableResultCode::ofSuccess);
    }

    @Override
    public CompletableResultCode forceFlush() {
        return handleDelegate(SpanProcessor::forceFlush, CompletableResultCode::ofSuccess);
    }

    @Override
    public void close() {
        if (delegate != null) {
            delegate.close();
        }
        resetDelegate();
    }

    /**
     * Clear the {@code delegate} and reset {@code warningLogged}.
     */
    private void resetDelegate() {
        delegate = null;
        warningLogged = false;
    }
}
