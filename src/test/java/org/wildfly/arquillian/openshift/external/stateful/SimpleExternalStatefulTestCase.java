package org.wildfly.arquillian.openshift.external.stateful;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.arquillian.openshift.api.Constants;
import org.wildfly.arquillian.openshift.api.WildFlyServerDescriptor;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

@RunWith(Arquillian.class)
public class SimpleExternalStatefulTestCase {

    public static final String PRINCIPAL = "alice";
    public static final String PASSWORD = "topsecret";

    private static StatefulRemote statefulBean = null;

    @Deployment(name = "service")
    public static WildFlyServerDescriptor service() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "service.jar");
        jar.addClasses(StatefulBean.class, StatefulRemote.class);
        return new WildFlyServerDescriptor(jar).setReplicas(2).setPrincipal(PRINCIPAL)
                .setPassword(PASSWORD);
    }

    @Test
    @RunAsClient
    public void test() throws Exception {
        //FIXME
        Thread.sleep(3000);
        try {
            if (statefulBean == null) {
                statefulBean = (StatefulRemote) findServiceBeanInCluster("service",
                        "StatefulBean!org.wildfly.arquillian.openshift.external.stateful.StatefulRemote");
            }
            for (int i = 0; i < 100; i++) {
                statefulBean.invoke();
            }
        } finally {
            statefulBean = null;
        }
    }

    static public Object findServiceBeanInCluster(String serviceName, String beanName) throws NamingException {
        Hashtable<String, String> table = new Hashtable<>();
        table.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        String providerUrl = "http://service-route-wildfly-testsuite.apps-crc.testing/wildfly-services";
        table.put(Context.PROVIDER_URL, providerUrl);
        table.put(Context.SECURITY_PRINCIPAL, PRINCIPAL);
        table.put(Context.SECURITY_CREDENTIALS, PASSWORD);

        InitialContext ic = new InitialContext(table);
        String beanQuery = "java:" + serviceName + "/" + beanName;
        return ic.lookup(beanQuery);
    }

}
