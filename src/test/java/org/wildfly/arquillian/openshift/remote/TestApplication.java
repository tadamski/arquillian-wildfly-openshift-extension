package org.wildfly.arquillian.openshift.remote;

import jakarta.ws.rs.core.Application;

import jakarta.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class TestApplication extends Application {

}
