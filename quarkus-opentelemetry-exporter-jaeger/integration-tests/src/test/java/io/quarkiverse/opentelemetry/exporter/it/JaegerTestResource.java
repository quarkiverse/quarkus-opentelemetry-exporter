package io.quarkiverse.opentelemetry.exporter.it;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;

import java.util.Map;

public class JaegerTestResource implements QuarkusTestResourceLifecycleManager {

    private static final int QUERY_PORT = 16686;
    private static final int COLLECTOR_PORT = 14250;
    private static final int HEALTH_PORT = 14269;

    private GenericContainer jaegerContainer;

    @Override
    public void init(Map<String, String> initArgs) {
        QuarkusTestResourceLifecycleManager.super.init(initArgs);
        jaegerContainer = new GenericContainer<>("ghcr.io/open-telemetry/opentelemetry-java/jaeger:1.32")
                .withImagePullPolicy(PullPolicy.alwaysPull())
                .withExposedPorts(COLLECTOR_PORT, QUERY_PORT, HEALTH_PORT)
                .waitingFor(Wait.forHttp("/").forPort(HEALTH_PORT));
    }

    @Override
    public Map<String, String> start() {
        jaegerContainer.start();
        final Map<String, String> properties = Map.of(
                "quarkus.jaeger.port", "" + jaegerContainer.getMappedPort(QUERY_PORT),
                "quarkus.opentelemetry.tracer.exporter.jaeger.endpoint", "http://" + jaegerContainer.getHost() + ":" +
                        jaegerContainer.getMappedPort(COLLECTOR_PORT));
        return properties;
    }

    @Override
    public void stop() {
        jaegerContainer.stop();
    }
}
