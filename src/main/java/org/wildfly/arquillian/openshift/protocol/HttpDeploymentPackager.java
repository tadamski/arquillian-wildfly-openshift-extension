package org.wildfly.arquillian.openshift.protocol;

import java.util.Collection;

import org.jboss.arquillian.container.test.spi.TestDeployment;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.deployment.ProtocolArchiveProcessor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.arquillian.openshift.api.Constants;

public class HttpDeploymentPackager implements DeploymentPackager {

    @Override
    public Archive<?> generateDeployment(TestDeployment testDeployment,
            Collection<ProtocolArchiveProcessor> processors) {
        Archive<?> archive = testDeployment.getApplicationArchive();
        if (archive instanceof WebArchive) {
            WebArchive webArchive = (WebArchive) archive;
            webArchive.addPackage(Constants.class.getPackage());
            webArchive.addAsLibraries(testDeployment.getAuxiliaryArchives());
        } else if (archive instanceof JavaArchive) {
            JavaArchive javaArchive = (JavaArchive) archive;
            javaArchive.addPackage(Constants.class.getPackage());
            for (Archive<?> a : testDeployment.getAuxiliaryArchives()) {
                javaArchive.merge(a);
            }
        }
        return archive;
    }
}
