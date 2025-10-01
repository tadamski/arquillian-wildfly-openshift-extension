package org.wildfly.arquillian.openshift.remote;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/")
public class TestEndpoint {

    @GET
    @Produces("text/plain")
    public Response foo(@jakarta.ws.rs.core.Context UriInfo info) throws Exception {
        String param = info.getQueryParameters().getFirst("param");
        return Response.ok(param+"bar").build();
    }
}
