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

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
@QuarkusTestResource(GcpTraceSampleExporterTestResource.class)
class GcpTraceSampleExporterTest {

    @GrpcClient
    MockTraceServiceGrpc.MockTraceServiceBlockingStub mockTraceServiceGrpc;

    @Test
    public void testSampleSpanExporter() {
        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));

        List<Span> spansList = mockTraceServiceGrpc.listSpans(Empty.newBuilder().getDefaultInstanceForType()).getSpansList();
        Assertions.assertEquals(1, spansList.size());
        Map<String, AttributeValue> attributeMapMap = spansList.get(0).getAttributes().getAttributeMapMap();
        Assertions.assertEquals("/direct", attributeMapMap.get("/http/route").getStringValue().getValue());
    }

}
