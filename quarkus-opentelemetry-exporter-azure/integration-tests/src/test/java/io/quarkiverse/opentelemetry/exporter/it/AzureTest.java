package io.quarkiverse.opentelemetry.exporter.it;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(Profile.DynamicPort.class)
public class AzureTest {

    public static final int WIREMOCK_PORT_NUMBER = 8090;
    public static final int HTTP_PORT_NUMBER = 53602; // See application.properties file
    public static final String WIREMOCK_HOST = "localhost";
    private WireMockServer wireMockServer;

    @BeforeEach
    public void startWireMock() {
        wireMockServer = new WireMockServer(
                new WireMockConfiguration().port(HTTP_PORT_NUMBER).bindAddress("127.0.0.1"));

        wireMockServer.start();
    }

    @AfterEach
    public void stopWireMock() {
        wireMockServer.stop();
    }

    @Test
    void connectionTest() throws InterruptedException {

        wireMockServer.stubFor(
                //any(urlEqualTo("http://127.0.0.1:53602/export")
                any(urlMatching(".*"))
                        .withPort(HTTP_PORT_NUMBER)
                        .willReturn(aResponse().withStatus(200)));

        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));

        Thread.sleep(5_000);

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/export/v2.1/track")));

    }
}
