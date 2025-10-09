package org.wildfly.arquillian.openshift.incluster.transaction.local;

import jakarta.ejb.Local;

@Local
public interface Client {

    void performFooTransaction(String fooName) throws Exception;
}
