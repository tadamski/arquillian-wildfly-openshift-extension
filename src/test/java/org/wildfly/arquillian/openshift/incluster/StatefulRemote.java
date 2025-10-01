package org.wildfly.arquillian.openshift.incluster;

import jakarta.ejb.Remote;

@Remote
public interface StatefulRemote {
    int invoke();
}
