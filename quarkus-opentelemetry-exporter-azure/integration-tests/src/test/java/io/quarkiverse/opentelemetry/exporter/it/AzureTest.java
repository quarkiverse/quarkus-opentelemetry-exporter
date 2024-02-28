package io.quarkiverse.opentelemetry.exporter.it;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkiverse.wiremock.devservice.ConnectWireMock;
import io.quarkiverse.wiremock.devservice.WireMockConfigKey;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
@ConnectWireMock
@TestProfile(Profile.DynamicPort.class)
public class AzureTest {

    WireMock wiremock;

    @ConfigProperty(name = WireMockConfigKey.PORT)
    Integer port;

    @Test
    void connectionTest() {

        wiremock.register(post(urlEqualTo("/export"))
                .withPort(port)
                .willReturn(aResponse().withStatus(200)));

        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));


        wiremock.verify(1, getRequestedFor(urlEqualTo("http://localhost:" + port + "/export")));

    }
}
