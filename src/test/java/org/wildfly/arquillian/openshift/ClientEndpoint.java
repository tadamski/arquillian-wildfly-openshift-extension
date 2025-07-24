package org.wildfly.arquillian.openshift;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Response.Status;

@Path("/")
public class ClientEndpoint {

    public static final String PRINCIPAL = "alice";
    public static final String PASSWORD = "topsecret";

    private static StatefulRemote statefulBean = null;

    @GET
    @Produces("text/plain")
    public Response foo(@jakarta.ws.rs.core.Context UriInfo info) throws Exception {
        try {
            if (statefulBean == null) {
                statefulBean = (StatefulRemote) findServiceBean("service",
                        "StatefulBean!org.wildfly.arquillian.openshift.StatefulRemote");
            }
            for (int i = 0; i < 100; i++) {
                statefulBean.invoke();
            }
            return Response.ok("OK").build();
        } catch (Exception e) {
            return Response.ok(e).build();
        } finally {
            statefulBean = null;
        }
    }

    static public Object findServiceBean(String serviceName, String beanName) throws NamingException {
        Hashtable<String, String> table = new Hashtable<>();
        table.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        String providerUrl = "http://" + serviceName + "-haproxy." + Constants.OPENSHIFT_NAMESPACE
                + ".svc.cluster.local:8080/wildfly-services";
        table.put(Context.PROVIDER_URL, providerUrl);
        table.put(Context.SECURITY_PRINCIPAL, PRINCIPAL);
        table.put(Context.SECURITY_CREDENTIALS, PASSWORD);

        InitialContext ic = new InitialContext(table);
        String beanQuery = "java:" + serviceName + "/" + beanName;
        return ic.lookup(beanQuery);
    }

}
