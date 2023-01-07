package io.quarkiverse.opentelemetry.exporter.it;

import java.util.Map;

import org.testcontainers.containers.wait.strategy.Wait;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class GcpTraceBatchExporterTestResource implements QuarkusTestResourceLifecycleManager {

    private CloudOperationsMockContainer cloudOperationsMockContainer;

    @Override
    public void init(Map<String, String> initArgs) {
        QuarkusTestResourceLifecycleManager.super.init(initArgs);
        cloudOperationsMockContainer = new CloudOperationsMockContainer()
                .withExposedPorts(18181)
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
                "quarkus.grpc.clients.mockTraceServiceGrpc.host", cloudOperationsMockContainer.getHost(),
                "quarkus.grpc.clients.mockTraceServiceGrpc.port", cloudOperationsMockContainer.getFirstMappedPort().toString());
    }

    @Override
    public void stop() {
        cloudOperationsMockContainer.stop();
    }
}
