package io.quarkiverse.opentelemetry.exporter.it;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class ExporterResource {

    @ConfigProperty(name = "quarkus.jaeger.port")
    String jaegerPort;

    @GET
    @Path("/get-port")
    public Response reset() {
        return Response.ok().entity(jaegerPort).build();
    }
}
