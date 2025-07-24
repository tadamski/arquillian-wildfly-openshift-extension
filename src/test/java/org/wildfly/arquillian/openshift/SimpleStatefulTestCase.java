package org.wildfly.arquillian.openshift;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

@RunWith(Arquillian.class)
public class SimpleStatefulTestCase {

    @Deployment(name = "client")
    public static WebArchive client() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "client.war");
        war.addClasses(ClientRestApplication.class, ClientEndpoint.class, StatefulRemote.class, Constants.class);
        return war;
    }

    @Deployment(name = "service")
    public static WildFlyServerDescriptor service() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "service.jar");
        jar.addClasses(StatefulBean.class, StatefulRemote.class);
        return new WildFlyServerDescriptor(jar).setReplicas(2).setPrincipal(ClientEndpoint.PRINCIPAL)
                .setPassword(ClientEndpoint.PASSWORD);
    }

    @Test
    public void test() {
        Client client = ClientBuilder.newClient();
        String result = client.target("http://client-route-wildfly-testsuite.apps-crc.testing/client")
                .request("text/plain").get(String.class);
        Assert.assertEquals("OK", result);

    }

}
