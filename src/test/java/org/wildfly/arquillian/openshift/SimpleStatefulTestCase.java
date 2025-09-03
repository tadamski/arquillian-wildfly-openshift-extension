package org.wildfly.arquillian.openshift;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@RunWith(Arquillian.class)
public class SimpleStatefulTestCase {

    public static final String PRINCIPAL = "alice";
    public static final String PASSWORD = "topsecret";

    private static StatefulRemote statefulBean = null;

    @Deployment(name = "client")
    public static WebArchive client() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "client.war");
        war.addClasses(StatefulRemote.class, Constants.class);
        return war;
    }

    @Deployment(name = "service")
    public static WildFlyServerDescriptor service() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "service.jar");
        jar.addClasses(StatefulBean.class, StatefulRemote.class);
        return new WildFlyServerDescriptor(jar).setReplicas(2).setPrincipal(PRINCIPAL)
                .setPassword(PASSWORD);
    }

    @Test
    @OperateOnDeployment("client")
    public void test() throws Exception {
        try {
            if (statefulBean == null) {
                statefulBean = (StatefulRemote) findServiceBean("service",
                        "StatefulBean!org.wildfly.arquillian.openshift.StatefulRemote");
            }
            for (int i = 0; i < 100; i++) {
                statefulBean.invoke();
            }
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
