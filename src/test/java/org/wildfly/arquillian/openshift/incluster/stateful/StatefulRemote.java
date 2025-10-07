package org.wildfly.arquillian.openshift.incluster.stateful;

import jakarta.ejb.Remote;

@Remote
public interface StatefulRemote {
    int invoke();
}
