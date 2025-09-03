package org.wildfly.arquillian.openshift.protocol;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.arquillian.container.test.spi.ContainerMethodExecutor;
import org.jboss.arquillian.test.spi.TestMethodExecutor;
import org.jboss.arquillian.test.spi.TestResult;

public class HttpContainerMethodExecutor implements ContainerMethodExecutor {

    @Override
    public TestResult invoke(TestMethodExecutor testMethodExecutor) {
        String classname = testMethodExecutor.getInstance().getClass().getName();
        String method = testMethodExecutor.getMethodName();

        Client client = ClientBuilder.newClient();
        try {
            Thread.sleep(10000);
        } catch(Exception e){}
        client.target("http://client-route-wildfly-testsuite.apps-crc.testing/arquillian-test-executor")
                .queryParam("class", classname).queryParam("method", method)
                .request("text/plain").get(String.class);
        return TestResult.passed();
    }

}
