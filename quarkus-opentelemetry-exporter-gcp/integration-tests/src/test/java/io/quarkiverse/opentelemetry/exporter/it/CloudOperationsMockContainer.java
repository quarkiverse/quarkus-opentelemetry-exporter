package io.quarkiverse.opentelemetry.exporter.it;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

class CloudOperationsMockContainer
        extends GenericContainer<CloudOperationsMockContainer> {
    CloudOperationsMockContainer() {
        super(
                new ImageFromDockerfile()
                        .withDockerfileFromBuilder(
                                builder -> builder
                                        .from("golang:1.17")
                                        .run("go install github.com/googleinterns/cloud-operations-api-mock/cmd@v2-alpha")
                                        .cmd("cmd --address=:18181")
                                        .build()));
        this.withExposedPorts(18181).waitingFor(Wait.forLogMessage(".*Listening on.*\\n", 1));
    }
}
