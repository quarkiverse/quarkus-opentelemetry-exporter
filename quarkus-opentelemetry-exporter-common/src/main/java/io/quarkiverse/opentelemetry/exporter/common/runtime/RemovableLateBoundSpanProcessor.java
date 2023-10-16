package io.quarkiverse.opentelemetry.exporter.common.runtime;

/**
 * The only point in having this class is to allow {@link TracerProviderCustomizer}
 * to easily ignore the configured {@link LateBoundSpanProcessor}.
 */
public final class RemovableLateBoundSpanProcessor extends LateBoundSpanProcessor {

    public static final RemovableLateBoundSpanProcessor INSTANCE = new RemovableLateBoundSpanProcessor();

    private RemovableLateBoundSpanProcessor() {
        super(null);
    }
}
