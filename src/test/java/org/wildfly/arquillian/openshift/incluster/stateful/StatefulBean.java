package org.wildfly.arquillian.openshift.incluster.stateful;

import jakarta.ejb.Stateful;

@Stateful
public class StatefulBean implements StatefulRemote {

        int invocationCount = 0;
        public int invoke(){
            invocationCount++;
            return invocationCount;
        }
}
