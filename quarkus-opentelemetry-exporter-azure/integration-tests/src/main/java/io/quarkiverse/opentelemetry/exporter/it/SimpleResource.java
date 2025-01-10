package io.quarkiverse.opentelemetry.exporter.it;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class SimpleResource {

    @Inject
    TracedService tracedService;

    @Inject
    OpenTelemetry openTelemetry;

    public static final String METER_SCOPE = "meter-scope";

    public static final String TEST_HISTOGRAM = "test-histogram";

    @GET
    public TraceData noPath() {
        TraceData data = new TraceData();
        data.message = "No path trace";
        return data;
    }

    @GET
    @Path("/direct")
    public TraceData directTrace() {
        Meter meter = openTelemetry.getMeter(METER_SCOPE);
        LongHistogram histogram = meter.histogramBuilder(TEST_HISTOGRAM).ofLongs().build();
        histogram.record(10);

        TraceData data = new TraceData();
        data.message = "Direct trace";

        return data;
    }

    @GET
    @Path("/chained")
    public TraceData chainedTrace() {
        TraceData data = new TraceData();
        data.message = tracedService.call();

        return data;
    }

    @GET
    @Path("/deep/path")
    public TraceData deepUrlPathTrace() {
        TraceData data = new TraceData();
        data.message = "Deep url path";

        return data;
    }

    @GET
    @Path("/param/{paramId}")
    public TraceData pathParameters(@PathParam("paramId") String paramId) {
        TraceData data = new TraceData();
        data.message = "ParameterId: " + paramId;

        return data;
    }
}
