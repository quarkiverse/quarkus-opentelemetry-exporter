package io.quarkiverse.opentelemetry.exporter.it;

import io.quarkiverse.wiremock.devservice.WireMockConfigKey;
import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

class Profile {

    public static class DynamicPort implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.log.category.\"io.quarkiverse\".level", "DEBUG");
        }
    }

    public static class FixedPort implements QuarkusTestProfile {

        public static final String PORT = "60000";

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.log.category.\"io.quarkiverse\".level", "DEBUG", WireMockConfigKey.PORT, PORT);
        }
    }

}