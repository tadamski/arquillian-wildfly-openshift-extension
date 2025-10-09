package org.wildfly.arquillian.openshift.incluster.transaction.local;

import jakarta.persistence.Id;

import jakarta.persistence.Entity;

@Entity
public class Foo {

    @Id
    int id;

    String name;

    int barCount = 0;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBarCount() {
        return barCount;
    }

    public void incrementBarCount() {
        this.barCount++;
    }
}
