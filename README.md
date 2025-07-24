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

## SimpleStatefulTestCase

As an example of extension behavior _SimpleStatefulTestCase_ was provided:
```
  @Deployment(name = "client")
    public static WebArchive client() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "client.war");
        war.addClasses(ClientRestApplication.class, ClientEndpoint.class, StatefulRemote.class, Constants.class);
        return war;
    }

    @Deployment(name = "service")
    public static WildFlyServerDescriptor service() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "service.jar");
        jar.addClasses(StatefulBean.class, StatefulRemote.class);
        return new WildFlyServerDescriptor(jar).setReplicas(2).setPrincipal(ClientEndpoint.PRINCIPAL)
                .setPassword(ClientEndpoint.PASSWORD);
    }
```
This is the key part of the tests. Those two deployments are going to be deployed into an OpenShift as two independent services. The difference between bare 
metal usage is introduction of _WildFlyServiceDescriptor_ which is a class that allows for configuration of OpenShift service deployment, you can also create 
deployment with Archive in which case minimal service configuration would be used as in _client_ example below.

```
    @Test
    public void test() {
        Client client = ClientBuilder.newClient();
        String result = client.target("http://client-route-wildfly-testsuite.apps-crc.testing/client")
                .request("text/plain").get(String.class);
        Assert.assertEquals("OK", result);

    }
```
The test itself runs in local VM and connects to client node via the route and verifies whether stateful session affinity is handled correctly. You can sanity check
the test by removing stickiness configuration parameters from ha-proxy template - the test is going to fail when one invocations hits the node without the session.

## Current shortcommings
The main purpose of initial version of this extension is to assess usefulness of above API for our usecases, as a result the internal code requires ammendments:
* optimization - the tests runs slowly - I plan to optimize Galleon/Docker images usage to improve the execution time of the test
* integration with maven-surefire-plugin - the version of artifact used had to be obtained from surefire plugin currently running the test
