package com.google.cloud.opentelemetry.trace;

public class TestTraceConfigurator {
    private static final String PROJECT_ID = "project-id";

    public static TraceConfiguration.Builder getTestTraceConfiguratorBuilder() {
        return TraceConfiguration.builder()
                .setInsecureEndpoint(true)
                .setProjectId(PROJECT_ID);
    }

}
