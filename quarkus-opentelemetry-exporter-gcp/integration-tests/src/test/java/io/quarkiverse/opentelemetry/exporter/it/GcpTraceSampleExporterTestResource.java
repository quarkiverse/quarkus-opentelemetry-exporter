package io.quarkiverse.opentelemetry.exporter.it;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.Map;

public class GcpTraceSampleExporterTestResource implements QuarkusTestResourceLifecycleManager {

    private CloudOperationsMockContainer cloudOperationsMockContainer;

    @Override
    public void init(Map<String, String> initArgs) {
        QuarkusTestResourceLifecycleManager.super.init(initArgs);
        cloudOperationsMockContainer = new CloudOperationsMockContainer()
                .waitingFor(Wait.forLogMessage(".*Listening on.*\\n", 1));
    }

    @Override
    public Map<String, String> start() {
        cloudOperationsMockContainer.start();
        return Map.of(
                "quarkus.opentelemetry.tracer.exporter.gcp.enabled", "true",
                "quarkus.opentelemetry.tracer.exporter.gcp.endpoint",
                String.format("%s:%d", cloudOperationsMockContainer.getHost(),
                        cloudOperationsMockContainer.getFirstMappedPort()),
                "quarkus.opentelemetry.tracer.exporter.gcp.cloudrun", "true",
                "quarkus.grpc.clients.mockTraceServiceGrpc.host", cloudOperationsMockContainer.getHost(),
                "quarkus.grpc.clients.mockTraceServiceGrpc.port", cloudOperationsMockContainer.getFirstMappedPort().toString());
    }

    @Override
    public void stop() {
        cloudOperationsMockContainer.stop();
    }
}
