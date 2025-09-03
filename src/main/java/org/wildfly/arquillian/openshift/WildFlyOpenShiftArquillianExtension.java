package org.wildfly.arquillian.openshift;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.wildfly.arquillian.openshift.protocol.TestExecutorDeployer;
import org.wildfly.arquillian.openshift.protocol.HttpProtocol;

public class WildFlyOpenShiftArquillianExtension implements LoadableExtension {

    public void register(ExtensionBuilder builder) {
        builder.service(DeployableContainer.class, WildFlyOpenShiftContainer.class);
        builder.service(Protocol.class, HttpProtocol.class);
        builder.observer(TestExecutorDeployer.class);
    }
}
