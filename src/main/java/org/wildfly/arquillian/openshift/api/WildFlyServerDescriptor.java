package org.wildfly.arquillian.openshift.api;

import java.io.OutputStream;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.shrinkwrap.descriptor.api.DescriptorExportException;

public class WildFlyServerDescriptor implements Descriptor {

    private final Archive<?> archive;
    private String deploymentName;
    private final Set<String> layers = new TreeSet<>();
    private String configHash;
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

    public String getDeploymentName() {
        if (deploymentName == null) {
            deploymentName = archive.getName().substring(0, archive.getName().lastIndexOf("."));
        }
        return deploymentName;
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

    public String getServerName() {
        if (configHash == null) {
            configHash = calculateConfigHash();
        }
        return "server-" + configHash;
    }

    private String calculateConfigHash() {
        StringBuilder sb = new StringBuilder();
        for (String l : layers) {
            sb.append(l);
        }
        String result = "";
        try {
            result = encodeSHA256(
                    MessageDigest.getInstance("SHA-256").digest(sb.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ignored) {}
        return result;
    }

    private String encodeSHA256(byte[] bytes) {
        return new BigInteger(1, bytes).toString(16).substring(0, 10);
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
