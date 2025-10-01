package org.wildfly.arquillian.openshift.protocol;

import java.lang.reflect.Method;

// import org.jboss.arquillian.junit.container.JUnitTestRunner;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/")
public class TestExecutorEndpoint {

    @GET
    @Produces("text/plain")
    public Response foo(@jakarta.ws.rs.core.Context UriInfo info) throws Exception {
        String className = info.getQueryParameters().getFirst("class");
        String methodName = info.getQueryParameters().getFirst("method");
        Class<?> testClass = TestExecutorEndpoint.class.getClassLoader().loadClass(className);
        Method method = testClass.getDeclaredMethod(methodName, new Class<?>[0]);
        method.invoke(testClass.newInstance(), new Object[0]);
        return Response.ok().build();
    }
}
