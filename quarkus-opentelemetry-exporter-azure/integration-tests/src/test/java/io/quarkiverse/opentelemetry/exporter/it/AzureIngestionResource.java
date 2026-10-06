package io.quarkiverse.opentelemetry.exporter.it;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Fake Application Insights ingestion endpoint, started before the application so that no telemetry (not even the
 * start-up logs) is ever persisted by the Azure SDK offline storage and replayed later.
 * <p>
 * The Azure SDK offline storage of the test JVM is also purged before the application starts: it is shared across
 * test runs and replays stale payloads from previous runs (one file every 30 seconds) into whatever listens on the
 * ingestion port, which can mask or cause failures.
 * <p>
 * To diagnose export issues, set the {@code com.azure.monitor.opentelemetry.autoconfigure.implementation.pipeline}
 * and {@code io.opentelemetry.sdk.metrics.export} log categories to {@code DEBUG} (level and min-level): they print
 * every batch handed to the ingestion pipeline and the periodic metric reader decisions.
 */
public class AzureIngestionResource implements QuarkusTestResourceLifecycleManager {

    public static final int PORT = 53602; // see application.properties
    public static final String TRACK_PATH = "/export/v2.1/track";
    public static final String CONNECTION_STRING = "InstrumentationKey=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx;IngestionEndpoint=http://127.0.0.1:"
            + PORT + "/export";

    private static final Path SYSTEM_TMP_DIR = Path.of("/tmp");

    private static volatile WireMockServer server;

    public static WireMockServer wireMock() {
        WireMockServer current = server;
        if (current == null) {
            throw new IllegalStateException("The Azure ingestion WireMock server is not running");
        }
        return current;
    }

    @Override
    public Map<String, String> start() {
        purgeAzureOfflineStorage();
        WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(PORT));
        wireMockServer.start();
        wireMockServer.stubFor(any(urlMatching(".*")).willReturn(aResponse().withStatus(200)));
        server = wireMockServer;
        return Map.of("applicationinsights.connection.string", CONNECTION_STRING);
    }

    @Override
    public void stop() {
        WireMockServer current = server;
        server = null;
        if (current != null) {
            current.stop();
        }
    }

    /**
     * Deletes the files persisted by the Azure SDK on failed sends, under
     * {@code <java.io.tmpdir>/applicationinsights/{telemetry,statsbeat}}.
     * <p>
     * Only the temporary directory of the test JVM is touched, which surefire and failsafe point to {@code target/}.
     * The system temporary directory ({@code /tmp}) is shared with every other Application Insights application of
     * the user and is never purged: should the test JVM run with the default temporary directory, nothing is deleted.
     * The storage of a native executable, which uses the system temporary directory, is therefore not purged either.
     */
    static void purgeAzureOfflineStorage() {
        Path tmpDir = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
        if (tmpDir.equals(SYSTEM_TMP_DIR)) {
            return;
        }
        Path root = tmpDir.resolve("applicationinsights");
        for (String subDir : List.of("telemetry", "statsbeat")) {
            deleteFiles(root.resolve(subDir));
        }
    }

    private static void deleteFiles(Path dir) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
