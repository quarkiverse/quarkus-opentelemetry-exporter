package io.quarkiverse.opentelemetry.exporter.it;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@QuarkusTestResource(JaegerTestResource.class)
public class JaegerExporterTest {

    public static final String SERVICE_NAME = "opentelemetry-exporter-jaeger-integration-test";

    @ConfigProperty(name = "quarkus.jaeger.port")
    String jaegerPort;

    @ConfigProperty(name = "quarkus.jaeger.host")
    String jaegerHost;

    /**
     * Response format example:
     * result = {LinkedHashMap@11959} size = 5
     * "data" -> {ArrayList@11969} size = 0
     * "total" -> {Integer@11971} 0
     * "limit" -> {Integer@11971} 0
     * "offset" -> {Integer@11971} 0
     * "errors" -> null
     */
    private Map<String, Object> getJaegerTrace() {
        Map<String, Object> as = get("/api/traces?service=" + SERVICE_NAME)
                .body().as(new TypeRef<>() {
                });
        return as;
    }

    @Test
    public void jaegerExtensionTest() {
        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));

        RestAssured.port = Integer.parseInt(jaegerPort);
        RestAssured.baseURI = String.format("http://%s", jaegerHost);

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .until(() -> ((List) getJaegerTrace().get("data")).size() > 0);

        final List data = (List) getJaegerTrace().get("data");
        final Map<String, Object> actual = (Map<String, Object>) data.get(0);
        final List spans = (List) actual.get("spans");

        assertEquals(1, data.size());
        assertEquals(1, spans.size());
        assertEquals("/direct", ((Map<String, Object>) spans.get(0)).get("operationName"));
    }

}
