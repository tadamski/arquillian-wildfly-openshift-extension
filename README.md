# Arquillian extension for testing WildFly applications on OpenShift

The purpose of creating this extension was to be able to create WildFly tests which are going to run on OpenShift but are structurally very similiar to regular, baremetal WildFly tests.

## Configuration

Prerequisites:
* OpenShift local installed (crc command) (I had networking problems on current 2.52 and recommend downgrading to 2.49)
* Docker installed
* Helm installed
* wildfly-operator project checked out from GitHub

  OpenShift local preparation:
  ```
  export OPERATOR_DIR=... # your wildfly-operator dir
  ./setup_crc.sh
  ./prepare_project.sh
  ```

  Running the test:
  ```
  mvn clean test
  ```

## In cluster test example

As an example of internal cluster test _SimpleStatefulTestCase_ was provided:
```
    @Deployment(name = "client")
    public static WebArchive client() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "client.war");
        war.addClass(StatefulRemote.class);
        return war;
    }

    @Deployment(name = "service")
    public static WildFlyServerDescriptor service() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "service.jar");
        jar.addClasses(StatefulBean.class, StatefulRemote.class);
        return new WildFlyServerDescriptor(jar).setReplicas(2).setPrincipal(PRINCIPAL)
                .setPassword(PASSWORD);
    }
```
This is the key part of the tests. Those two deployments are going to be deployed into an OpenShift as two independent services. The difference between bare metal usage is introduction of _WildFlyServiceDescriptor_ which is a class that allows for configuration of OpenShift service deployment, you can also create deployment with Archive in which case minimal service configuration would be used as in _client_ example below.

```
    @Test
    @OperateOnDeployment("client")
    public void test() throws Exception {
        try {
            if (statefulBean == null) {
                statefulBean = (StatefulRemote) findServiceBean("service",
                        "StatefulBean!org.wildfly.arquillian.openshift.incluster.StatefulRemote");
            }
            for (int i = 0; i < 100; i++) {
                statefulBean.invoke();
            }
        } finally {
            statefulBean = null;
        }
    }
```
The test will be run on the deployment specificed by @OperateOnDeployment annotation in the same it is done in baremetal test.

You can sanity check the test by removing stickiness configuration parameters from ha-proxy template - the test is going to fail when one invocations hits the node without the session.

## Outside cluster test example

As an example of test that is run from outstide the cluster _OutsideClusterInvocationTestCase_ was provided. The test is annotated with _@RunAsClient_ annotation:

```
@RunWith(Arquillian.class)
@RunAsClient
public class OutsideClusterInvocationTestCase {
```

The test method is executed from the VM in which the test runs:

```
    @Test
    public void test() {
        Client client = ClientBuilder.newClient();
        String result = client.target("http://service-route-wildfly-testsuite.apps-crc.testing/service").queryParam("param", "foo")
                .request("text/plain").get(String.class);
        Assert.assertEquals("foobar", result);
    }
```

## Current shortcommings
The main purpose of initial version of this extension is to assess usefulness of above API for our usecases, as a result the internal code requires ammendments:
* integration with maven-surefire-plugin - the version of artifact used had to be obtained from surefire plugin currently running the test
