package org.wildfly.arquillian.openshift.remote;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class OutsideClusterInvocationTestCase {

    public static final String PRINCIPAL = "alice";
    public static final String PASSWORD = "topsecret";

    @Deployment(name = "service")
    public static WebArchive service() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "service.war");
        war.addClasses(TestApplication.class, TestEndpoint.class);
        return war;
    }

    @Test
    public void test() {
        Client client = ClientBuilder.newClient();
        String result = client.target("http://service-route-wildfly-testsuite.apps-crc.testing/service").queryParam("param", "foo")
                .request("text/plain").get(String.class);
        Assert.assertEquals("foobar", result);
    }
}
