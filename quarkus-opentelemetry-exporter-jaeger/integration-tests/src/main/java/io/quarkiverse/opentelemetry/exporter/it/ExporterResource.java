package io.quarkiverse.opentelemetry.exporter.it;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
