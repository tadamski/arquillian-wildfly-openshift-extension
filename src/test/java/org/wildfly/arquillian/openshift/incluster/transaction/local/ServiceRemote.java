package org.wildfly.arquillian.openshift.incluster.transaction.local;

import jakarta.ejb.Remote;

@Remote
public interface ServiceRemote {
    void createFoo(String fooName);
    void incrementBarCount(String fooName);
    int getBarCount(String fooName);
}
