package io.quarkiverse.opentelemetry.exporter.it;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

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
    }

    @AfterEach
    public void stopWireMock() {
        wireMockServer.stop();
    }

    @Test
    void connectionTest() throws InterruptedException {

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
                .atMost(Duration.ofSeconds(10))
                .until(telemetryDataContainTheHttpCall(wireMockServer));

        telemetryDataContainTheOTelMetric(wireMockServer);

        // Non regression test for https://github.com/Azure/azure-sdk-for-java/issues/41040
        Thread.sleep(10_000);
        List<LoggedRequest> telemetryExport = wireMockServer.findAll(postRequestedFor(urlEqualTo("/export/v2.1/track")));
        List<String> requestBodies = telemetryExport
                .stream()
                .map(request -> new String(request.getBody())).toList();
        requestBodies.stream().forEach(System.out::println); // It's convenient to print the telemetry data on the console to spot potential issues
        Optional<String> telemetryDataExport = requestBodies.stream()
                .filter(body -> body.contains("RemoteDependency") && body.contains("POST /export/v2.1/track"))
                .findAny();
        assertThat(telemetryDataExport).as("Telemetry export request should not appear as a dependency.").isEmpty();
    }

    private static Callable<Boolean> telemetryDataContainTheHttpCall(WireMockServer wireMockServer) {
        return () -> wireMockServer.findAll(postRequestedFor(urlEqualTo("/export/v2.1/track")))
                .stream()
                .map(request -> new String(request.getBody()))
                .anyMatch(body -> body.contains("Request") && body.contains("GET /direct"));
    }

    private static Callable<Boolean> telemetryDataContainTheOTelMetric(WireMockServer wireMockServer) {
        return () -> wireMockServer.findAll(postRequestedFor(urlEqualTo("/export/v2.1/track")))
                .stream()
                .map(request -> new String(request.getBody()))
                .anyMatch(body -> body.contains("Metric") && body.contains("baseData\":{\"ver\":2,\"metrics\":[{\"name\":\"" + SimpleResource.TEST_HISTOGRAM + "\",\"value\":10.0,\"count\":1,\"min\":10.0,\"max\":10.0"));
    }
}
