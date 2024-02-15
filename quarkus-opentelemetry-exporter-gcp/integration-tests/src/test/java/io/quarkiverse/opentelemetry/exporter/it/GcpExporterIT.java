package io.quarkiverse.opentelemetry.exporter.it;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@Disabled("The UdsNameResolverProvider is forced to be initialized at run time with no good reason. " +
        "This can be related to https://github.com/oracle/graal/pull/8230.")
public class GcpExporterIT extends GcpTraceBatchExporterTest {
}
