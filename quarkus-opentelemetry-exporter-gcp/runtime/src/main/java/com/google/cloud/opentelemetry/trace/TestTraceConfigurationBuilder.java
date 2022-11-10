package com.google.cloud.opentelemetry.trace;

import java.util.HashMap;
import java.util.Map;

import com.google.devtools.cloudtrace.v2.AttributeValue;

public class TestTraceConfigurationBuilder {
    private static final String PROJECT_ID = "project-id";
    private static final Map<String, AttributeValue> FIXED_ATTRIBUTES = new HashMap<>();

    public static TraceConfiguration.Builder buildTestTraceConfiguration() {
        return TraceConfiguration.builder()
                .setInsecureEndpoint(true)
                .setFixedAttributes(FIXED_ATTRIBUTES)
                .setProjectId(PROJECT_ID);
    }

}
