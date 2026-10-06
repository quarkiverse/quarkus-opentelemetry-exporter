package io.quarkiverse.opentelemetry.exporter.it;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = AzureIngestionResource.class, restrictToAnnotatedClass = true)
public class AzureTest {

    /** Must match quarkus.otel.metric.export.interval in application.properties. */
    static final Duration METRIC_EXPORT_INTERVAL = Duration.ofSeconds(2);

    static final Duration TIMEOUT = Duration.ofSeconds(30);

    /**
     * The histogram value is fixed, the count depends on how many times /direct was called within an export interval
     * (delta temporality).
     */
    static final Pattern TEST_HISTOGRAM = Pattern.compile(
            "\"name\":\"" + SimpleResource.TEST_HISTOGRAM
                    + "\",\"value\":\\d+\\.\\d+,\"count\":\\d+,\"min\":10\\.0,\"max\":10\\.0");

    @Test
    void connectionTest() {
        callDirect();

        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertThat(receivedBodies())
                        .as("Trace of the GET /direct request. Received: %s", summary())
                        .anyMatch(body -> body.contains("Request") && body.contains("GET /direct")
                                && body.contains(":dsq999-SNAPSHOT")));

        // Metrics must keep flowing. This is the regression test for the metric reader being wired to the Azure
        // no-op marker exporter instead of the real one (non-deterministic SDK customizer ordering): in that case not
        // a single Metric payload is ever sent, without any warning.
        int metricPayloadsBefore = metricPayloadCount();
        await().atMost(TIMEOUT)
                .pollInterval(METRIC_EXPORT_INTERVAL)
                .untilAsserted(() -> {
                    callDirect(); // delta temporality: every call records a new histogram data point
                    assertThat(metricPayloadCount())
                            .as("No metric export reached the ingestion endpoint: the periodic metric reader is not "
                                    + "wired to the Azure Monitor exporter. Received: %s", summary())
                            .isGreaterThanOrEqualTo(metricPayloadsBefore + 2);
                });

        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertThat(receivedBodies())
                        .as("Should contain OTel metric. Received: %s", summary())
                        .anyMatch(body -> body.contains("Metric") && TEST_HISTOGRAM.matcher(body).find()));

        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertThat(receivedBodies())
                        .as("Should contain OTel log. Received: %s", summary())
                        .anyMatch(AzureTest::isStartupLog));

        // Non regression test for https://github.com/Azure/azure-sdk-for-java/issues/41040: the export requests
        // themselves must not be traced as dependencies (see AzureEndpointSampler). Several exports have been sent by
        // now; keep checking for a few span export cycles.
        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(receivedBodies())
                        .as("Telemetry export request should not appear as a dependency.")
                        .noneMatch(body -> body.contains("RemoteDependency")
                                && body.contains("POST " + AzureIngestionResource.TRACK_PATH)));
    }

    private static void callDirect() {
        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));
    }

    private static List<String> receivedBodies() {
        return AzureIngestionResource.wireMock()
                .findAll(postRequestedFor(urlEqualTo(AzureIngestionResource.TRACK_PATH)))
                .stream()
                .map(request -> new String(request.getBody()))
                .collect(Collectors.toList());
    }

    private static int metricPayloadCount() {
        return (int) receivedBodies().stream().filter(body -> body.contains("\"name\":\"Metric\"")).count();
    }

    private static boolean isStartupLog(String body) {
        return body.contains("\"message\":\"opentelemetry-exporter-azure-integration-test")
                && body.contains("(powered by Quarkus ")
                && body.contains("started in")
                && body.contains(
                        "{\"LoggerName\":\"io.quarkus.opentelemetry\",\"LoggingLevel\":\"INFO\",\"log.logger.namespace\":\"org.jboss.logging.Logger\"");
    }

    /** Telemetry item kinds received so far, e.g. {Message=3, Metric=5, Request=1}. */
    private static Map<String, Long> summary() {
        Pattern kind = Pattern.compile("\"name\":\"(Message|Metric|Request|RemoteDependency|Exception|Event)\"");
        return receivedBodies().stream()
                .flatMap(body -> kind.matcher(body).results().map(result -> result.group(1)))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }
}
