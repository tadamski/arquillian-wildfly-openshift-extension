package org.wildfly.arquillian.openshift.incluster.transaction.local;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.arquillian.openshift.api.WildFlyServerDescriptor;

import javax.naming.InitialContext;
import javax.naming.NamingException;

@RunWith(Arquillian.class)
public class LocalTransactionTestCase {

    public static final String PRINCIPAL = "alice";
    public static final String PASSWORD = "topsecret";

    @Deployment(name = "client")
    public static WebArchive client() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "client.war");
        war.addClasses(Client.class, ClientBean.class, ServiceRemote.class);
        war.addAsManifestResource("wildfly-config.xml");
        return war;
    }

    @Deployment(name = "service")
    public static WildFlyServerDescriptor service() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "service.jar");
        jar.addClasses(ServiceRemote.class, ServiceBean.class, Foo.class);
        jar.addAsManifestResource("persistence.xml");
        return new WildFlyServerDescriptor(jar).addLayers("h2-datasource").setReplicas(2).setPrincipal(PRINCIPAL)
                .setPassword(PASSWORD);
    }

    @Test
    @OperateOnDeployment("client")
    public void test() throws Exception {
        Client client = findClientBean();
        String fooName = "foo";
        client.performFooTransaction(fooName);
    }

    private Client findClientBean() throws NamingException {
        return (Client) new InitialContext().lookup("java:global/client/ClientBean");
    }
}
