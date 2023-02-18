package io.quarkiverse.opentelemetry.exporter.it;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@QuarkusTestResource(JaegerTestResource.class)
public class JaegerExporterTest {

    public static final String SERVICE_NAME = "opentelemetry-exporter-jaeger-integration-test";
    public static final String BASE_URI = "http://localhost";

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
        // native test needs to get port from inside the app.
        String jaegerPort = given()
                .contentType("application/json")
                .when().get("/get-port")
                .then()
                .statusCode(200)
                .extract().as(String.class);

        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));

        RestAssured.port = Integer.parseInt(jaegerPort);
        RestAssured.baseURI = BASE_URI;

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .until(() -> ((List) getJaegerTrace().get("data")).size() > 0);

        final List traces = (List) getJaegerTrace().get("data");

        assertEquals(2, traces.size());
        final Map<String, Object> getPortTrace = getTrace(traces, "/get-port");
        assertTrace(getPortTrace);
        final Map<String, Object> directTrace = getTrace(traces, "/direct");
        assertTrace(directTrace);
    }

    private Map<String, Object> getTrace(final List data, final String operationNameValue) {
        return (Map<String, Object>) data.stream()
                .filter(entry -> ((Map<String, Object>) ((List) ((Map<String, Object>) entry)
                        .get("spans")).get(0))
                        .get("operationName").equals("/direct"))
                .findFirst()
                .orElse(Optional.empty());
    }

    private void assertTrace(final Map<String, Object> trace) {
        assertFalse(trace.isEmpty(), "trace cannot be empty");
        List spans = (List) trace.get("spans");
        assertEquals(1, spans.size());
    }

}
