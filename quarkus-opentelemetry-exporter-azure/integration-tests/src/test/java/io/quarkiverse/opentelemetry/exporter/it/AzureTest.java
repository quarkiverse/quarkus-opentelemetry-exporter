package io.quarkiverse.opentelemetry.exporter.it;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.time.Duration;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

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
    void connectionTest() {

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

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .until(azureExportIsDone(wireMockServer));
    }

    private static Callable<Boolean> azureExportIsDone(WireMockServer wireMockServer) {
        return () -> {
            try {
                wireMockServer.verify(1, postRequestedFor(urlEqualTo("/export/v2.1/track")));
                return Boolean.TRUE;
            } catch (AssertionError e) {
                return Boolean.FALSE;
            }
        };
    }
}
