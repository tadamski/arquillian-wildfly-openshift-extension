package org.wildfly.arquillian.openshift.protocol;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.event.container.AfterDeploy;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.shrinkwrap.api.Archive;
import org.wildfly.arquillian.openshift.WildFlyOpenShiftContainer;
import org.wildfly.arquillian.openshift.api.WildFlyServerDescriptor;

public class TestExecutorDeployer {

    private static final Logger log = Logger.getLogger(WildFlyOpenShiftContainer.class.getName());

    public synchronized void doServiceDeploy(@Observes(precedence = 1) AfterDeploy event, Container container) {
        try {
            DeployableContainer<?> deployableContainer = container.getDeployableContainer();
            WildFlyOpenShiftContainer wosc = (WildFlyOpenShiftContainer) deployableContainer;
            Archive<?> archive;
            int replicas;
            if (event.getDeployment().getArchive() != null) {
                archive = event.getDeployment().getArchive();
                replicas = 1;
            } else {
                WildFlyServerDescriptor descriptor = (WildFlyServerDescriptor) event.getDeployment().getDescriptor();
                archive = descriptor.getArchive();
                replicas = descriptor.getReplicas();
            }
            wosc.deployArquillianTestExecutor(event.getDeployment().getName(), archive.getName(), replicas);
        } catch (Throwable t) {
            log.log(Level.WARNING, "Unable do deploy Arquillian Test Executor", t);
        }
    }
}
