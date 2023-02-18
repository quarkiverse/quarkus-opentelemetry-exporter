package io.quarkiverse.opentelemetry.exporter.it;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigResource {

    @ConfigProperty(name = "container.host")
    String containerAddress;

    @ConfigProperty(name = "container.port")
    Integer containerPort;

    @GET
    @Path("/host")
    public Response host() {
        return Response.ok().entity(containerAddress).build();
    }

    @GET
    @Path("/port")
    public Response port() {
        return Response.ok().entity(containerPort).build();
    }
}
