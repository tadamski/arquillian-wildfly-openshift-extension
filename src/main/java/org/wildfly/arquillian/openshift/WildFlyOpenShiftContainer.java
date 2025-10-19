package org.wildfly.arquillian.openshift;

import static org.wildfly.arquillian.openshift.api.Constants.DEFAULT_MAVEN_REPO_PATH;
import static org.wildfly.arquillian.openshift.api.Constants.HAPROXY_CHART;
import static org.wildfly.arquillian.openshift.api.Constants.OPENSHIFT_NAMESPACE;
import static org.wildfly.arquillian.openshift.api.Constants.OPENSHIFT_PASSWORD;
import static org.wildfly.arquillian.openshift.api.Constants.OPENSHIFT_PRINCIPAL;
import static org.wildfly.arquillian.openshift.api.Constants.OPENSHIFT_REGISTRY_URL;
import static org.wildfly.arquillian.openshift.api.Constants.OPENSHIFT_REGISTRY_USERNAME;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.galleon.ProvisioningException;
import org.jboss.galleon.api.GalleonBuilder;
import org.jboss.galleon.api.GalleonFeaturePack;
import org.jboss.galleon.api.Provisioning;
import org.jboss.galleon.api.ProvisioningBuilder;
import org.jboss.galleon.api.config.GalleonProvisioningConfig;
import org.jboss.galleon.maven.plugin.util.MavenArtifactRepositoryManager;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.slf4j.simple.SimpleLogger;
import org.w3c.dom.Document;
import org.wildfly.arquillian.openshift.api.Constants;
import org.wildfly.arquillian.openshift.api.WildFlyServerDescriptor;
import org.wildfly.arquillian.openshift.protocol.TestExecutorApplication;
import org.wildfly.arquillian.openshift.protocol.TestExecutorEndpoint;
import org.wildfly.plugin.tools.GalleonUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.marcnuri.helm.Helm;
import com.marcnuri.helm.InstallCommand;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;

public class WildFlyOpenShiftContainer implements DeployableContainer<WildFlyOpenShiftArquillianConfiguration> {

    private static final Logger log = Logger.getLogger(WildFlyOpenShiftContainer.class.getName());

    private final GalleonBuilder galleonBuilder;
    private final DockerClientConfig dockerClientConfig;
    private final Yaml yaml;
    private final Config openShiftConfig;

    public WildFlyOpenShiftContainer() throws Exception {
        this.galleonBuilder = configureGalleonBuilder();
        this.dockerClientConfig = createDockerClientConfig();
        this.yaml = createYaml();
        this.openShiftConfig = createOpenShiftConfig();
    }

    private GalleonBuilder configureGalleonBuilder() throws NoLocalRepositoryManagerException, ProvisioningException {

        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        RepositorySystem system = locator.getService(RepositorySystem.class);

        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepository = new LocalRepository(
                System.getenv("HOME") + Constants.DEFAULT_MAVEN_REPO_PATH);
        LocalRepositoryManager localRepositoryManager = new EnhancedLocalRepositoryManagerFactory().newInstance(session,
                localRepository);
        session.setLocalRepositoryManager(localRepositoryManager);

        GalleonBuilder galleonBuilder = new GalleonBuilder();
        galleonBuilder.addArtifactResolver(new MavenArtifactRepositoryManager(system, session));
        return galleonBuilder;
    }

    private DockerClientConfig createDockerClientConfig() {
        return DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerTlsVerify(false)
                .withRegistryUsername(OPENSHIFT_REGISTRY_USERNAME)
                .withRegistryPassword(getOpenShiftBearerToken())
                .withRegistryUrl(OPENSHIFT_REGISTRY_URL)
                .build();
    }

    private String getOpenShiftBearerToken() {
        try (OpenShiftClient client = new KubernetesClientBuilder().withConfig(openShiftConfig).build()
                .adapt(OpenShiftClient.class)) {
            return client.getConfiguration().getAutoOAuthToken();
        }
    }

    private Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        return new Yaml(options);
    }

    private Config createOpenShiftConfig() {
        return new ConfigBuilder().withUsername(OPENSHIFT_PRINCIPAL).withPassword(OPENSHIFT_PASSWORD).build();
    }

    @Override
    public Class<WildFlyOpenShiftArquillianConfiguration> getConfigurationClass() {
        return WildFlyOpenShiftArquillianConfiguration.class;
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("OpenShiftHTTP");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        try {
            deploy(new WildFlyServerDescriptor(archive));
            return new ProtocolMetaData();
        } catch (Exception e) {
            throw new DeploymentException(e.getMessage(), e);
        }
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
        try {
            if (!(descriptor instanceof WildFlyServerDescriptor)) {
                throw new IllegalArgumentException("descriptor must be an instance of WildflyServerDescriptor");
            }
            WildFlyServerDescriptor serverDescriptor = (WildFlyServerDescriptor) descriptor;
            Archive<?> archive = serverDescriptor.getArchive();

            boolean provisioned = provisionServer(serverDescriptor);
            final InputStream input = archive.as(ZipExporter.class).exportAsInputStream();
            java.nio.file.Files.copy(input,
                    new File("target/" + archive.getName()).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            if (provisioned) {
                installDockerImage(serverDescriptor.getServerName());
            }
            deployWildFlyServer(serverDescriptor);
            // FIXME
            // wait for deployments - proper callback would be great
            Thread.sleep(3000);
        } catch (Exception e) {
            throw new DeploymentException(e.getMessage(), e);
        }
    }

    private boolean provisionServer(WildFlyServerDescriptor serverDescriptor)
            throws URISyntaxException, ProvisioningException, NoLocalRepositoryManagerException, IOException {

        GalleonProvisioningConfig config = buildGalleonConfig(galleonBuilder, serverDescriptor.getLayers());
        ProvisioningBuilder builder = galleonBuilder.newProvisioningBuilder(config);

        String serverName = serverDescriptor.getServerName();

        log.info("Provisioning of server " + serverName + " for deployment " + serverDescriptor.getDeploymentName()
                + " started");

        File serverFile = new File(serverName);
        if (serverFile.exists()) {
            log.info("Server present in cache - skipping the build");
            return false;
        } else {
            try (Provisioning pm = builder.setInstallationHome(Path.of(serverName)).build()) {
                pm.provision(config);
            }
            return true;
        }
    }

    private GalleonProvisioningConfig buildGalleonConfig(GalleonBuilder galleonBuilder, Set<String> layersSet)
            throws ProvisioningException, IOException {

        // ugly hack - but wasn't able to find any point of integrating with surefire mojo/repository
        Properties mavenProperties = new Properties();
        mavenProperties.load(new FileInputStream("target/maven.properties"));
        String wildflyVersion = mavenProperties.getProperty("version.wildfly");

        List<GalleonFeaturePack> featurePacks = new ArrayList<>();

        GalleonFeaturePack wildflyPack = new GalleonFeaturePack();
        wildflyPack.setLocation(String.format("org.wildfly:wildfly-galleon-pack:%s", wildflyVersion));
        featurePacks.add(wildflyPack);

        GalleonFeaturePack datasourcesPack = new GalleonFeaturePack();
        datasourcesPack.setLocation("org.wildfly:wildfly-datasources-galleon-pack:11.1.0.Final-SNAPSHOT");
        featurePacks.add(datasourcesPack);

        List<String> layers = new ArrayList<>();
        layers.addAll(layersSet);

        List<String> excludedLayers = new ArrayList<>();

        Map<String, String> galleonOptions = new HashMap<>();
        galleonOptions.put("jboss-fork-embedded", "true");

        GalleonProvisioningConfig config = GalleonUtils.buildConfig(galleonBuilder, featurePacks, layers,
                excludedLayers, galleonOptions,
                "standalone.xml");
        return config;
    }

    private void installDockerImage(String serverName) throws InterruptedException {
        DockerClient client = DockerClientBuilder.getInstance(dockerClientConfig).build();

        String imageTag = String.format("%s/%s/%s:latest", dockerClientConfig.getRegistryUrl(), OPENSHIFT_NAMESPACE,
                serverName);

        log.info("Building Docker image for server " + serverName);
        // Dockerfile declaration doesn't work normally (new File("Dockerfile")) because
        // of some docker-java bug
        client.buildImageCmd().withDockerfile(new File(new File("Dockerfile").getAbsolutePath()))
                .withBuildArg("server_dir", serverName)
                .withTag(imageTag).exec(new BuildImageResultCallback()).awaitCompletion();

        log.info("Pushing Docker image for deployment " + serverName);
        client.pushImageCmd(imageTag)
                .withAuthConfig(new AuthConfig().withUsername(dockerClientConfig.getRegistryUsername())
                        .withPassword(dockerClientConfig.getRegistryPassword())
                        .withRegistryAddress(dockerClientConfig.getRegistryUrl()))
                .exec(new PushImageResultCallback()).awaitCompletion();

    }

    private void deployWildFlyServer(WildFlyServerDescriptor serverDescriptor)
            throws FileNotFoundException, IOException, InterruptedException, ExecutionException, TimeoutException {
        String deploymentName = serverDescriptor.getDeploymentName();
        generateOperatorYaml(serverDescriptor);
        try (OpenShiftClient client = new KubernetesClientBuilder().withConfig(openShiftConfig).build()
                .adapt(OpenShiftClient.class)) {
            log.info("Deploying WildFlyServer for deployment " + deploymentName);
            client.load(
                    new FileInputStream(new File(
                            String.format("target/%s-operator.yaml",
                                    deploymentName))))
                    .inNamespace(OPENSHIFT_NAMESPACE).create();
            for (int i = 0; i < serverDescriptor.getReplicas(); i++) {
                log.info("Waiting for pod " + deploymentName + "-" + i);
                client.pods().inNamespace(OPENSHIFT_NAMESPACE).withName(deploymentName + "-" +
                        i).waitUntilReady(100, TimeUnit.SECONDS);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                client.pods().inNamespace(OPENSHIFT_NAMESPACE).withName(deploymentName + "-" + i)
                        .file("/opt/wildfly/standalone/deployments/" + serverDescriptor.getArchive().getName())
                        .upload(Path.of("target", serverDescriptor.getArchive().getName()));
                if ((serverDescriptor.getPrincipal() != null) && (serverDescriptor.getPassword() != null)) {
                    log.info("Adding application user for pod " + deploymentName + "-" + i);
                    client.pods().inNamespace(OPENSHIFT_NAMESPACE).withName(deploymentName + "-" + i)
                            .writingOutput(baos).writingError(baos)
                            .exec(String.format("/opt/wildfly/bin/add-user.sh -a %s %s",
                                    serverDescriptor.getPrincipal(), serverDescriptor.getPassword()).split(" "));
                }
            }
        }

        deployProxy(deploymentName, serverDescriptor.getReplicas());
    }

    public void deployArquillianTestExecutor(String deploymentName, String archiveName, int replicas)
            throws IOException {
        WebArchive serviceArchive = ShrinkWrap.create(WebArchive.class, "arquillian-test-executor.war");
        serviceArchive.addClass(TestExecutorApplication.class);
        serviceArchive.addClass(TestExecutorEndpoint.class);
        serviceArchive.addAsManifestResource(createDeploymentStructure(archiveName), "jboss-deployment-structure.xml");
        final InputStream input = serviceArchive.as(ZipExporter.class).exportAsInputStream();
        java.nio.file.Files.copy(input,
                new File("target/" + serviceArchive.getName()).toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        try (OpenShiftClient client = new KubernetesClientBuilder().withConfig(openShiftConfig).build()
                .adapt(OpenShiftClient.class)) {
            log.info("Adding arquillian service for deployment " + deploymentName);
            for (int i = 0; i < replicas; i++) {
                log.info("Waiting for pod " + deploymentName + "-" + i);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                client.pods().inNamespace(OPENSHIFT_NAMESPACE).withName(deploymentName + "-" + i)
                        .file("/opt/wildfly/standalone/deployments/" + serviceArchive.getName())
                        .upload(Path.of("target", serviceArchive.getName()));
            }
        }
    }

    private StringAsset createDeploymentStructure(String deploymentName) {
        return new StringAsset(String.format(
                "<jboss-deployment-structure xmlns=\"urn:jboss:deployment-structure:1.2\">\n" + //
                        "    <deployment>\n" + //
                        "        <dependencies>\n" + //
                        "            <module name=\"deployment.%s\"/>\n" + //
                        "        </dependencies>\n" + //
                        "    </deployment>\n" + //
                        "</jboss-deployment-structure>",
                deploymentName));

    }

    @SuppressWarnings("unchecked")
    private void generateOperatorYaml(WildFlyServerDescriptor serverDescriptor)
            throws FileNotFoundException, IOException {
        String deploymentName = serverDescriptor.getDeploymentName();
        log.info("Generating WildFlyServer Yaml for deployment " + deploymentName);

        Map<String, Object> data = yaml.load(new FileInputStream("operator-template.yaml"));
        ((Map<String, Object>) data.get("metadata")).put("name", deploymentName);
        Map<String, Object> specMap = (Map<String, Object>) data.get("spec");
        specMap.put("applicationImage",
                String.format("%s/%s/%s:latest", dockerClientConfig.getRegistryUrl(), OPENSHIFT_NAMESPACE,
                        serverDescriptor.getServerName()));
        specMap.put("replicas", serverDescriptor.getReplicas());
        if (!serverDescriptor.getSystemProperties().isEmpty()) {
            specMap.put("env", serverDescriptor.getSystemProperties());
        }
        FileWriter writer = new FileWriter(
                String.format("target/%s-operator.yaml", deploymentName));
        yaml.dump(data, writer);
    }

    private void deployProxy(String deploymentName, int replicas) throws FileNotFoundException, IOException {
        generateProxyYaml(deploymentName, replicas);
        log.info("Installing HAProxy for deployment " + deploymentName);
        InstallCommand ic = Helm.install(HAPROXY_CHART);
        ic.withName(deploymentName).withValuesFile(Path.of("target", deploymentName + "-proxy.yaml"));
        ic.call();
    }

    @SuppressWarnings("unchecked")
    private void generateProxyYaml(String deploymentName, int replicas) throws FileNotFoundException, IOException {
        log.info("Generating HAProxy Yaml for deployment " + deploymentName);
        Map<String, Object> data = yaml.load(new FileInputStream(
                "proxy-template.yaml"));
        StringBuilder csb = new StringBuilder((String) data.get("config"));
        for (int i = 0; i < replicas; i++) {
            csb.append(String.format(
                    "  server node%d {{ .Release.Name }}-%d.{{ .Release.Name }}-headless.%s.svc.cluster.local\n", i, i,
                    OPENSHIFT_NAMESPACE));
        }
        data.put("config", csb.toString());
        FileWriter writer = new FileWriter(
                String.format("target/%s-proxy.yaml", deploymentName));
        yaml.dump(data, writer);
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
    }
}
