package org.wildfly.arquillian.openshift.protocol;

import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.test.spi.ContainerMethodExecutor;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.container.test.spi.command.CommandCallback;

public class HttpProtocol<T extends HttpProtocolConfiguration> implements Protocol<T> {

    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getProtocolConfigurationClass() {
        return (Class<T>) HttpProtocolConfiguration.class;
    }

    @Override
    public ProtocolDescription getDescription() {
        return new ProtocolDescription("OpenShiftHTTP");
    }

    @Override
    public DeploymentPackager getPackager() {
        return new HttpDeploymentPackager();
    }

    @Override
    public ContainerMethodExecutor getExecutor(T protocolConfiguration,
            ProtocolMetaData metaData, CommandCallback callback) {
        return new HttpContainerMethodExecutor();
    }
}
