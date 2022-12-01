package io.quarkiverse.opentelemetry.exporter.it;

import api.MockTraceServiceGrpc;
import com.google.devtools.cloudtrace.v2.AttributeValue;
import com.google.devtools.cloudtrace.v2.Span;
import com.google.protobuf.Empty;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
@QuarkusTestResource(GcpTraceBatchExporterTestResource.class)
class GcpTraceBatchExporterTest {

    @GrpcClient
    MockTraceServiceGrpc.MockTraceServiceBlockingStub mockTraceServiceGrpc;

    @Test
    public void gcpExtensionTest() {
        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .until(() -> (mockTraceServiceGrpc.listSpans(Empty.newBuilder().getDefaultInstanceForType()).getSpansList()
                        .size() > 0));

        List<Span> spansList = mockTraceServiceGrpc.listSpans(Empty.newBuilder().getDefaultInstanceForType()).getSpansList();
        List<Span> mockApplicationSpans = spansList.stream()
                .filter(sp -> !sp.getDisplayName().getValue().equals("Sent.api.MockTraceService/ListSpans"))
                .collect(Collectors.toList());

        Assertions.assertEquals(1, mockApplicationSpans.size());
        Map<String, AttributeValue> attributeMapMap = mockApplicationSpans.get(0).getAttributes().getAttributeMapMap();
        Assertions.assertEquals("/direct", attributeMapMap.get("/http/route").getStringValue().getValue());
    }

}
