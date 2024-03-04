package io.quarkiverse.opentelemetry.exporter.it;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

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
    public static final int HTTPS_PORT_NUMBER = 53602; // See application.properties file
    public static final String WIREMOCK_HOST = "localhost";
    private WireMockServer wireMockServer;

    @BeforeEach
    public void startWireMock() {
        wireMockServer = new WireMockServer(
                new WireMockConfiguration().httpsPort(HTTPS_PORT_NUMBER).port(WIREMOCK_PORT_NUMBER));
        wireMockServer.start();
        configureFor(WIREMOCK_HOST, WIREMOCK_PORT_NUMBER);
    }

    @AfterEach
    public void stopWireMock() {
        wireMockServer.stop();
    }

    @Test
    void connectionTest() throws InterruptedException {

        wireMockServer.stubFor(any(urlEqualTo("https:///localhost:" + HTTPS_PORT_NUMBER + "/export/v2.1/track"))
                .withPort(HTTPS_PORT_NUMBER)
                .willReturn(aResponse().withStatus(200)));

        Thread.sleep(30_000);

        wireMockServer.verify(1, getRequestedFor(urlEqualTo("http://localhost:" + HTTPS_PORT_NUMBER + "/export")));

    }
}
