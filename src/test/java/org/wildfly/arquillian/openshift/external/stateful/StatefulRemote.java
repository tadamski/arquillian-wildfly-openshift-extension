package org.wildfly.arquillian.openshift.external.stateful;

import jakarta.ejb.Remote;

@Remote
public interface StatefulRemote {
    int invoke();
}
