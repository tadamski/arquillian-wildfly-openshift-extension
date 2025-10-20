package org.wildfly.arquillian.openshift.incluster.transaction.local;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.junit.Assert;
import org.wildfly.arquillian.openshift.api.Constants;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

@Stateless
public class ClientBean implements Client {

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void performFooTransaction(String fooName) throws Exception {
        ServiceRemote service = (ServiceRemote) findServiceBean("service",
                "ServiceBean!org.wildfly.arquillian.openshift.incluster.transaction.local.ServiceRemote");
        service.createFoo(fooName);
        for(int i = 0; i < 30; i++) {
            service.incrementBarCount(fooName);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void checkBarCount(String fooName, int expectedBarCount) throws Exception {
        ServiceRemote service = (ServiceRemote) findServiceBean("service",
                "ServiceBean!org.wildfly.arquillian.openshift.incluster.transaction.local.ServiceRemote");
        Assert.assertEquals(expectedBarCount, service.getBarCount(fooName));
    }


    static public Object findServiceBean(String serviceName, String beanName) throws NamingException {
        Hashtable<String, String> table = new Hashtable<>();
        table.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        String providerUrl = "http://" + serviceName + "-haproxy." + Constants.OPENSHIFT_NAMESPACE
                + ".svc.cluster.local:8080/wildfly-services";
        table.put(Context.PROVIDER_URL, providerUrl);
        table.put(Context.SECURITY_PRINCIPAL, LocalTransactionTestCase.PRINCIPAL);
        table.put(Context.SECURITY_CREDENTIALS, LocalTransactionTestCase.PASSWORD);

        InitialContext ic = new InitialContext(table);
        String beanQuery = "java:" + serviceName + "/" + beanName;
        return ic.lookup(beanQuery);
    }
}
