# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Quarkiverse project providing Quarkus extensions that export OpenTelemetry telemetry to third-party observability vendors. These are OpenTelemetry SDK plugins implementing the Exporter interface. Three exporters are shipped: **Azure** (Azure Monitor), **GCP** (Google Cloud, tracing only, *not* supported in native mode), and **Sentry**.

The extension version tracks the Quarkus version at each release (since v3.5.0). The `quarkus.version` property in the root `pom.xml` is the source of truth, and several other dependency versions (netty, opentelemetry-instrumentation-alpha) must be kept in sync with the quarkus-bom — see the `Start`/`End` comment block in `pom.xml`.

## Build & test commands

```bash
# Full build (this is what CI runs first — formatting is enforced)
mvn -B formatter:validate clean install

# Native build/verify (CI runs this second; note GCP is excluded from native)
mvn -B -Dnative verify

# Auto-fix formatting before committing (formatter:validate will fail the build otherwise)
mvn formatter:format

# Build a single module (plus its dependencies)
mvn install -pl quarkus-opentelemetry-exporter-azure/runtime -am

# Run a single test class
mvn test -pl quarkus-opentelemetry-exporter-azure/deployment -Dtest=AzureExporterEnabledTest

# Run a single integration test (@QuarkusIntegrationTest, runs against the packaged app)
mvn verify -pl quarkus-opentelemetry-exporter-azure/integration-tests -Dit.test=AzureIT
```

Java 17 is required (`maven.compiler.release` = 17). CI only runs on Linux.

## Module architecture

Each exporter is a standard Quarkus extension split into Maven modules:

- `.../runtime` — code that runs in the application: `@ConfigMapping` config interfaces, `@Recorder` classes producing beans at RUNTIME_INIT, and exporter/sampler/span-processor implementations.
- `.../deployment` — the `*Processor` with `@BuildStep`s executed at build time. Unit tests (`@QuarkusUnitTest`) live here.
- `.../integration-tests` — a runnable Quarkus app plus `@QuarkusTest` / `@QuarkusIntegrationTest` (the latter covers native).

`quarkus-opentelemetry-exporter-common` is the exception: a plain runtime-only jar (no deployment module) holding shared span-processor types (`LateBoundSpanProcessor`, `RemovableLateBoundSpanProcessor`) used across exporters.

## How an exporter plugs into Quarkus OpenTelemetry

These extensions do **not** replace the core `quarkus-opentelemetry` extension — they extend it. The wiring pattern (see `AzureExporterProcessor`) is:

1. A `BooleanSupplier` gates the whole processor via `@BuildSteps(onlyIf = ...)`, driven by a `quarkus.otel.<vendor>.enabled` config flag. Config classes use `@ConfigMapping(prefix = "quarkus.otel.<vendor>")` and typically map a legacy `quarkus.opentelemetry.tracer.exporter.<vendor>.enabled` property for backwards compatibility.
2. `ExternalOtelExporterBuildItem("<vendor>")` tells core OTel that an external exporter is present.
3. A `SyntheticBeanBuildItem` recorded at `@Record(ExecutionTime.RUNTIME_INIT)` (with `@Consume(OpenTelemetrySdkBuildItem.class)`) registers an `AutoConfiguredOpenTelemetrySdkBuilderCustomizer` / `AutoConfigurationCustomizerProvider` and, where relevant, a `Sampler`. The `@Recorder` builds these from runtime config values.

When adding config or beans, follow this exact chain — a new exporter mimics an existing module end-to-end.

## Native image

Native support lives entirely in the deployment `*Processor` and (for GCP) GraalVM `Substitutions`. The Azure processor is the reference for the heavy native work: `RuntimeInitializedClassBuildItem`, `ReflectiveClassBuildItem`, `NativeImageProxyDefinitionBuildItem` (derived by scanning `@ServiceInterface` in the Jandex index), and `ServiceProviderBuildItem`. Any new vendor SDK dependency will likely need similar native registrations discovered by running `mvn -Dnative verify`. GCP is intentionally not built in native.

## Documentation

Docs are AsciiDoc under `docs/modules/ROOT/pages/`, built by the `docs` module (active unless `performRelease=true`). **Configuration reference pages are generated** from config classes into `target/` during the build and copied into `docs/.../includes/`. Do not hand-edit generated config docs. When adding a module, add a page and register it in `docs/modules/ROOT/nav.adoc`. See `CONTRIBUTING.md` for the per-registry doc scaffold.