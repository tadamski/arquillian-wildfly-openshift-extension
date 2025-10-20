package org.wildfly.arquillian.openshift.incluster.transaction.local;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.wildfly.arquillian.openshift.WildFlyOpenShiftContainer;

import java.util.logging.Logger;

@Stateless
public class ServiceBean implements ServiceRemote {

    private static final Logger log = Logger.getLogger(ServiceBean.class.getName());

    @PersistenceContext(unitName = "foopc")
    private EntityManager em;

    @Override
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void createFoo(String fooName) {
        log.info("ServiceBean#createFoo");
        Foo foo = new Foo();
        foo.setName(fooName);
        em.persist(foo);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void incrementBarCount(String fooName) {
        log.info("ServiceBean#incrementBarCount");
        Foo foo = findFooByName(fooName);
        foo.incrementBarCount();
        log.info("Incremented bar count to "+foo.getBarCount());
        em.persist(foo);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public int getBarCount(String fooName) {
        return findFooByName(fooName).getBarCount();
    }

    private Foo findFooByName(String fooName) {
       return (Foo) em.createQuery("select f from Foo f where f.name = :fooName").setParameter("fooName", fooName).getSingleResult();
    }
}
