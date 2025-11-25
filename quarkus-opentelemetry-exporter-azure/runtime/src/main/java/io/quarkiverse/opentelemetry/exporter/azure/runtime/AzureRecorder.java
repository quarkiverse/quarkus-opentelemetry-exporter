package io.quarkiverse.opentelemetry.exporter.azure.runtime;

import java.util.*;
import java.util.function.Function;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AzureRecorder {

    public Function<SyntheticCreationalContext<AzureMonitorCustomizer>, AzureMonitorCustomizer> createAzureMonitorCustomizer() {
        return new Function<>() {
            @Override
            public AzureMonitorCustomizer apply(
                    SyntheticCreationalContext<AzureMonitorCustomizer> objectSyntheticCreationalContext) {
                return new AzureMonitorCustomizer();
            }
        };
    }

    public Function<SyntheticCreationalContext<AzureEndpointSampler>, AzureEndpointSampler> createSampler() {
        return new Function<>() {
            @Override
            public AzureEndpointSampler apply(SyntheticCreationalContext<AzureEndpointSampler> context) {
                return new AzureEndpointSampler();
            }
        };
    }

}
