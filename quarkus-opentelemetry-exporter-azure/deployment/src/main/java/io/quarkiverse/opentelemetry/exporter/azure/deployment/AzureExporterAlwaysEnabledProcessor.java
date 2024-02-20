package io.quarkiverse.opentelemetry.exporter.azure.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

// Executed even if the extension is disabled, see https://github.com/quarkusio/quarkus/pull/26966/
public class AzureExporterAlwaysEnabledProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.OPENTELEMETRY_JAEGER_EXPORTER);
    } // To remove (legacy reasons)

}
