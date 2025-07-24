package org.wildfly.arquillian.openshift;

import jakarta.ejb.Remote;

@Remote
public interface StatefulRemote {
    int invoke();
}
