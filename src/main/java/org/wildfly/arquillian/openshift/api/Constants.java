package org.wildfly.arquillian.openshift.api;

public class Constants {

    private Constants() {
    }

    public static String DEFAULT_MAVEN_REPO_PATH = "/.m2/repository";
    public static String OPENSHIFT_NAMESPACE = "wildfly-testsuite";
    public static String OPENSHIFT_PRINCIPAL = "developer";
    public static String OPENSHIFT_PASSWORD = "developer";
    public static String OPENSHIFT_REGISTRY_USERNAME = "openshift";
    public static String OPENSHIFT_REGISTRY_URL = "default-route-openshift-image-registry.apps-crc.testing";
    public static String HAPROXY_CHART = "haproxytech/haproxy";
}
