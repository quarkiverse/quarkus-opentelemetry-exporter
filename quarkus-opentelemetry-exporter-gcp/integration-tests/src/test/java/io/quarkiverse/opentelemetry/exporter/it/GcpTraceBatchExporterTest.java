package io.quarkiverse.opentelemetry.exporter.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import com.google.devtools.cloudtrace.v2.AttributeValue;
import com.google.devtools.cloudtrace.v2.Span;
import com.google.protobuf.Empty;

import api.MockTraceServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(GcpTraceBatchExporterTestResource.class)
class GcpTraceBatchExporterTest {

    @Test
    public void gcpExtensionTest() {
        // native test needs to get port from inside the app.
        String mockContainerHost = given()
                .contentType("application/json")
                .when().get("/config/host")
                .then()
                .statusCode(200)
                .extract().body().asString();

        Integer mockContainerPort = given()
                .contentType("application/json")
                .when().get("/config/port")
                .then()
                .statusCode(200)
                .extract().as(Integer.class);

        ManagedChannel channel = getChannel(mockContainerHost, mockContainerPort);
        MockTraceServiceGrpc.MockTraceServiceBlockingStub mockTraceServiceGrpc = MockTraceServiceGrpc.newBlockingStub(channel);

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

        Assertions.assertEquals(3, mockApplicationSpans.size());
        Map<String, AttributeValue> hostAttributeMapMap = mockApplicationSpans.get(0).getAttributes().getAttributeMapMap();
        Assertions.assertEquals("/config/host", hostAttributeMapMap.get("/http/route").getStringValue().getValue());

        Map<String, AttributeValue> portAttributeMapMap = mockApplicationSpans.get(1).getAttributes().getAttributeMapMap();
        Assertions.assertEquals("/config/port", portAttributeMapMap.get("/http/route").getStringValue().getValue());

        Map<String, AttributeValue> attributeMapMap = mockApplicationSpans.get(2).getAttributes().getAttributeMapMap();
        Assertions.assertEquals("/direct", attributeMapMap.get("/http/route").getStringValue().getValue());
    }

    private ManagedChannel getChannel(String host, Integer port) {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }
}
