package org.wildfly.arquillian.openshift;

import org.kohsuke.MetaInfServices;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.core.spi.LoadableExtension;

@MetaInfServices(LoadableExtension.class)
public class WildFlyOpenShiftArquillianExtension implements LoadableExtension {

    public void register(ExtensionBuilder builder) {
        builder.service(DeployableContainer.class, WildFlyOpenShiftContainer.class);
    }
}
