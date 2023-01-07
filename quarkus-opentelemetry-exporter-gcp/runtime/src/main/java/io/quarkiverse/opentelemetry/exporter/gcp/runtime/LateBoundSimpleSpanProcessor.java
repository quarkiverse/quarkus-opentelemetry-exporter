package io.quarkiverse.opentelemetry.exporter.gcp.runtime;

import org.jboss.logging.Logger;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

public class LateBoundSimpleSpanProcessor implements SpanProcessor {
    private static final Logger log = Logger.getLogger(LateBoundSimpleSpanProcessor.class);

    private boolean warningLogged = false;
    private SimpleSpanProcessor delegate;

    /**
     * Set the actual {@link SimpleSpanProcessor} to use as the delegate.
     *
     * @param delegate Properly constructed {@link SimpleSpanProcessor} for processing spans.
     */
    public void setSimpleSpanProcessorDelegate(SimpleSpanProcessor delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        if (delegate == null) {
            logDelegateNotFound();
            return;
        }
        delegate.onStart(parentContext, span);
    }

    @Override
    public boolean isStartRequired() {
        if (delegate == null) {
            logDelegateNotFound();
            return false;
        }
        return delegate.isStartRequired();
    }

    @Override
    public void onEnd(ReadableSpan span) {
        if (delegate == null) {
            logDelegateNotFound();
            return;
        }
        delegate.onEnd(span);
    }

    @Override
    public boolean isEndRequired() {
        if (delegate == null) {
            logDelegateNotFound();
            return true;
        }
        return delegate.isEndRequired();
    }

    @Override
    public CompletableResultCode shutdown() {
        if (delegate == null) {
            logDelegateNotFound();
            return CompletableResultCode.ofSuccess();
        }
        return delegate.shutdown();
    }

    @Override
    public CompletableResultCode forceFlush() {
        if (delegate == null) {
            logDelegateNotFound();
            return CompletableResultCode.ofSuccess();
        }
        return delegate.forceFlush();
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

    /**
     * If we haven't previously logged an error,
     * log an error about a missing {@code delegate} and set {@code warningLogged=true}
     */
    private void logDelegateNotFound() {
        if (!warningLogged) {
            log.warn("No SimpleSpanProcessor delegate specified, no action taken.");
            warningLogged = true;
        }
    }
}
