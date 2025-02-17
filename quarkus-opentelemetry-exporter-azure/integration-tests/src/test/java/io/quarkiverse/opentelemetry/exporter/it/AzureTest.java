package io.quarkiverse.opentelemetry.exporter.it;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AzureTest {

    public static final int HTTP_PORT_NUMBER = 53602; // See application.properties file
    private WireMockServer wireMockServer;

    @BeforeEach
    public void startWireMock() {
        WireMockConfiguration wireMockConfiguration = new WireMockConfiguration().port(HTTP_PORT_NUMBER);
        wireMockServer = new WireMockServer(wireMockConfiguration);
        wireMockServer.start();
        wireMockServer.stubFor(
                any(urlMatching(".*"))
                        .withPort(HTTP_PORT_NUMBER)
                        .willReturn(aResponse().withStatus(200)));
    }

    @AfterEach
    public void stopWireMock() {
        wireMockServer.stop();
    }

    @Test
    void traceExport() {

        givenHttpCall();

        await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> requestBodiesOfTelemetryExport()
                        .anyMatch(
                                body -> body.contains("Request") && body.contains("GET /direct")
                                        && body.contains(":dsq999-SNAPSHOT")));
    }

    private static void givenHttpCall() {
        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));
    }

    private Stream<String> requestBodiesOfTelemetryExport() {
        return wireMockServer.findAll(postRequestedFor(urlEqualTo("/export/v2.1/track")))
                .stream()
                .map(request -> new String(request.getBody()));
    }

    @Test
    void logRecordExport() {
        await()
                .atMost(Duration.ofSeconds(90))
                .pollDelay(Duration.ofMillis(500))
                .until(() -> requestBodiesOfTelemetryExport()
                        .anyMatch(
                                body -> body.contains("MessageData") &&
                                        body.contains("\"message\":\"opentelemetry-exporter-azure-integration-test")
                                        && body.contains("(powered by Quarkus ")
                                        && body.contains("started in") && body.contains(
                                                "{\"LoggerName\":\"io.quarkus.opentelemetry\",\"LoggingLevel\":\"INFO\",\"log.logger.namespace\":\"org.jboss.logging.Logger\"")));
    }

    @Test
    void metricExport() {
        await()
                .atMost(Duration.ofSeconds(90))
                .pollDelay(Duration.ofMillis(500))
                .until(() -> requestBodiesOfTelemetryExport()
                        .anyMatch(
                                body -> body.contains("Metric")
                                        && body.contains("baseData\":{\"ver\":2,\"metrics\":[{\"name\":\""
                                                + SimpleResource.TEST_HISTOGRAM
                                                + "\",\"value\":10.0,\"count\":1,\"min\":10.0,\"max\":10.0")));
    }

    @Test
    // Non regression test for https://github.com/Azure/azure-sdk-for-java/issues/41040
    void exportRequestAbsenceTest() throws InterruptedException {

        givenHttpCall();

        await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> requestBodiesOfTelemetryExport()
                        .anyMatch(
                                body -> body.contains("Request") && body.contains("GET /direct")
                                        && body.contains(":dsq999-SNAPSHOT")));

        Thread.sleep(10_000);

        List<LoggedRequest> telemetryHttpRequests = wireMockServer.findAll(postRequestedFor(urlEqualTo("/export/v2.1/track")));
        List<String> requestBodies = telemetryHttpRequests
                .stream()
                .map(request -> new String(request.getBody())).toList();
        // Cannot be on by default but it's convenient to print the telemetry data on the console to spot potential issues:
        //requestBodies.stream().forEach(System.out::println);
        Optional<String> telemetryDataExport = requestBodies.stream()
                .filter(body -> body.contains("RemoteDependency") && body.contains("POST /export/v2.1/track"))
                .findAny();
        assertThat(telemetryDataExport).as("Telemetry export request should not appear as a dependency.").isEmpty();
    }

}
