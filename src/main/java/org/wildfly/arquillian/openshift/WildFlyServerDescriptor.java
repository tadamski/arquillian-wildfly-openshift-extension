package org.wildfly.arquillian.openshift;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.shrinkwrap.descriptor.api.DescriptorExportException;

public class WildFlyServerDescriptor implements Descriptor {

    private final Archive<?> archive;
    private final Set<String> layers = new HashSet<>();
    private int replicas;
    private String principal;
    private String password;

    public WildFlyServerDescriptor(Archive<?> archive) {
        this.archive = archive;
        layers.add("cloud-server");
        layers.add("naming");
        layers.add("ejb");
        replicas = 1;
    }

    public Archive<?> getArchive() {
        return archive;
    }

    public WildFlyServerDescriptor addLayers(String... layers) {
        for (String layer : layers) {
            this.layers.add(layer);
        }
        return this;
    }

    public Set<String> getLayers() {
        return layers;
    }

    public WildFlyServerDescriptor setReplicas(int replicas) {
        this.replicas = replicas;
        return this;
    }

    public int getReplicas() {
        return replicas;
    }

    public WildFlyServerDescriptor setPrincipal(String principal) {
        this.principal = principal;
        return this;
    }

    public String getPrincipal() {
        return principal;
    }

    public WildFlyServerDescriptor setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String getDescriptorName() {
        return archive.getName();
    }

    @Override
    public String exportAsString() throws DescriptorExportException {
        return archive.toString();
    }

    @Override
    public void exportTo(OutputStream output) throws DescriptorExportException, IllegalArgumentException {
        archive.writeTo(output, null);
    }

}
