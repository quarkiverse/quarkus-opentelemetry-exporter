package io.quarkiverse.opentelemetry.exporter.it;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SentryTest {

    public static final int HTTP_PORT_NUMBER = 53602; // See application.properties file
    private static final ObjectMapper mapper = new ObjectMapper();
    private WireMockServer wireMockServer;

    @BeforeEach
    public void startWireMock() {
        WireMockConfiguration wireMockConfiguration = new WireMockConfiguration().port(HTTP_PORT_NUMBER);
        wireMockServer = new WireMockServer(wireMockConfiguration);
        wireMockServer.start();
    }

    @AfterEach
    public void stopWireMock() {
        wireMockServer.stop();
    }

    @Test
    void traceTest() {

        wireMockServer.stubFor(
                any(urlMatching(".*"))
                        .withPort(HTTP_PORT_NUMBER)
                        .willReturn(aResponse().withStatus(200)));

        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));

        await()
                .atMost(Duration.ofSeconds(2))
                .until(telemetryDataContainTheHttpCall(wireMockServer, 1));

        List<List<JsonNode>> requestBodies = getRequestBodies();

        assertThat(requestBodies).hasSize(1)
                .satisfiesOnlyOnce(group -> {
                    assertThat(group).hasSize(3);
                    assertThat(group.get(0).get("sdk").get("name").asText()).isEqualTo("sentry.java");
                    assertThat(group.get(1).get("type").asText()).isEqualTo("transaction");
                    assertThat(group.get(2).get("transaction").asText()).isEqualTo("GET /direct");
                });

    }

    @Test
    void loggedTest() {

        wireMockServer.stubFor(
                any(urlMatching(".*"))
                        .withPort(HTTP_PORT_NUMBER)
                        .willReturn(aResponse().withStatus(200)));

        given()
                .contentType("application/json")
                .when().get("/logged")
                .then()
                .statusCode(200)
                .body("message", equalTo("Logged trace"));

        await()
                .atMost(Duration.ofSeconds(2))
                .until(telemetryDataContainTheHttpCall(wireMockServer, 2));

        List<List<JsonNode>> requestBodies = getRequestBodies();

        assertThat(requestBodies).hasSize(2);

        assertThat(requestBodies).satisfiesOnlyOnce(group -> {
            assertThat(group).hasSize(3);
            assertThat(group.get(0).get("sdk").get("name").asText()).isEqualTo("sentry.java");
            assertThat(group.get(1).get("type").asText()).isEqualTo("event");
            assertThat(group.get(2).get("message").get("message").asText()).isEqualTo("This is a logged message");
        })
                .satisfiesOnlyOnce(group -> {
                    assertThat(group).hasSize(3);
                    assertThat(group.get(0).get("sdk").get("name").asText()).isEqualTo("sentry.java");
                    assertThat(group.get(1).get("type").asText()).isEqualTo("transaction");
                    assertThat(group.get(2).get("transaction").asText()).isEqualTo("GET /logged");
                });

    }

    private @NotNull List<List<JsonNode>> getRequestBodies() {
        List<LoggedRequest> telemetryExport = wireMockServer.findAll(postRequestedFor(anyUrl()));
        List<List<JsonNode>> requestBodies = telemetryExport
                .stream()
                .map(request -> {
                    try {
                        List<JsonNode> parsed = new ArrayList<>();
                        String[] messages = new String(request.getBody()).split("\n");
                        for (String message : messages) {
                            parsed.add(mapper.readTree(message));
                        }
                        return parsed;
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
        return requestBodies;
    }

    private static Callable<Boolean> telemetryDataContainTheHttpCall(WireMockServer wireMockServer, int size) {
        return () -> wireMockServer.findAll(postRequestedFor(anyUrl())).size() == size;
    }
}
