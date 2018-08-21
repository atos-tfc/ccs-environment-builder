package org.slinkyframework.environment.docker;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slinkyframework.environment.builder.EnvironmentBuilderException;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.function.BiConsumer;

import static java.lang.String.format;

public class DockerDriver {
    private static final String ENVIRONMENT_DOCKER_HOST = "DOCKER_HOST";
    private static final String ENVIRONMENT_DOCKER_MACHINE_NAME = "DOCKER_MACHINE_NAME";

    private static final Logger LOG = LoggerFactory.getLogger(DockerDriver.class);

    private static final int ONE_SECOND = 1000;
    private static final long THIRTY_SECONDS = 30000;

    private final String containerName;
    private final String imageName;
    private final int[] ports;
    private final String dockerHostname;

    private DockerClient dockerClient;
    private String containerId;

    public DockerDriver(String containerName, String imageName, int[] ports) {
        this.containerName = containerName;
        this.imageName = imageName;
        this.ports = ports;

        verifyEnvironmentConfigured();

        dockerHostname = System.getenv(ENVIRONMENT_DOCKER_MACHINE_NAME);

        connectToDocker();
    }

    private void verifyEnvironmentConfigured() {
        if (!isEnvironmentVariableSet(ENVIRONMENT_DOCKER_HOST)
                || !isEnvironmentVariableSet(ENVIRONMENT_DOCKER_MACHINE_NAME)) {
            throw new EnvironmentBuilderException("Docker environment variables not set e.g. DOCKER_HOST. If using docker-machine run 'eval \"$(docker-machine env dev)\"' where 'dev' is your docker-machine host.");
        }
    }

    private boolean isEnvironmentVariableSet(String name) {
        String value = System.getenv(name);

        return value != null && !value.trim().equals("");
    }

    private void connectToDocker() {
        try {
            LOG.debug("Connecting to Docker");

            dockerClient = DefaultDockerClient.fromEnv().build();
            dockerClient.ping();

            LOG.debug("Connection to Docker established");

        } catch (DockerException | DockerCertificateException | InterruptedException e) {
            throw new EnvironmentBuilderException("Unable to connect to Docker", e);
        }
    }

    public void setUpDocker() {

        LOG.info("Setting up Docker container '{}'", containerName);

        pullContainer();

        Optional<Container> existingContainer = findExistingContainer();

        if (existingContainer.isPresent()) {
            LOG.warn("Container '{}' already exists", containerName);
            killAndRemoveContainer(existingContainer.get());
        }
        ContainerCreation container = createContainer();
        containerId = container.id();

        waitFor(this::startContainer);
        waitForContainerToStart();
    }

    private void pullContainer() {
        try {
            if (!findImage().isPresent()) {
                dockerClient.pull(imageName);
            }
        } catch (DockerException | InterruptedException e) {
            throw new EnvironmentBuilderException("Unable to pull container: " + imageName, e);
        }
    }

    public Optional<Container> findExistingContainer() {
        try {
            List<Container> containers = dockerClient.listContainers(DockerClient.ListContainersParam.allContainers(true));

            for (Container container : containers) {
                for (String name : container.names()) {
                    if (name.contains(containerName)) {
                        return Optional.of(container);
                    }
                }
            }

        } catch (DockerException | InterruptedException e) {
            LOG.error("Unable to retrieve a list of Docker containers", e);
        }
        return Optional.empty();
    }

    public void killAndRemoveContainer() {
        Optional<Container> existingContainer = findExistingContainer();

        if (existingContainer.isPresent()) {
            killAndRemoveContainer(existingContainer.get());
            LOG.info("Couchbase docker container '{}' killed and removed", containerName);
        } else {
            LOG.warn("Container '{}' was not running", containerName);
        }
    }

    private void killAndRemoveContainer(Container container) {
        try {
            if (container.status().startsWith("Up")) {
                LOG.debug("Killing Docker container '{}'", containerName);
                dockerClient.killContainer(container.id());
            }
            LOG.debug("Removing Docker container '{}'", containerName);
            dockerClient.removeContainer(container.id());
        } catch (DockerException | InterruptedException e) {
            throw new EnvironmentBuilderException("Unable to kill and remove a container", e);
        }
    }

    private ContainerCreation createContainer() {

        LOG.debug("Creating Docker container '{}'", containerName);

        final Map<String, List<PortBinding>> portBindings = new HashMap<>();
        for (int port : ports) {
            List<PortBinding> hostPorts = new ArrayList<>();
            hostPorts.add(PortBinding.of("0.0.0.0", port));
            portBindings.put(port + "/tcp", hostPorts);
        }

        HostConfig hostConfig = HostConfig.builder()
                .portBindings(portBindings)
                .build();

        // Create container
        ContainerConfig config = ContainerConfig.builder()
                .image(imageName)
                .hostConfig(hostConfig)
                .build();

        try {
            ContainerCreation container = dockerClient.createContainer(config, containerName);

            LOG.debug("Docker container '{}' created", containerName);
            return container;
        } catch (DockerException | InterruptedException e) {
            throw new EnvironmentBuilderException("Unable to create Docker container. Is one already running with the same name?", e);
        }
    }

    private void startContainer(DockerClient docker, String containerId) {

        LOG.info("Starting container '{}", containerName);

        try {
            docker.startContainer(containerId);

            LOG.debug("Container '{}' started", containerName);
        } catch (DockerException  | InterruptedException e) {
            LOG.error("Unable to start container '{}'. Is there something running on the same ports?", containerName);
            throw new EnvironmentBuilderException("Unable to start container", e);
        }
    }

    private void waitForContainerToStart() {
        TimeoutRetryPolicy retryPolicy = new TimeoutRetryPolicy();
        retryPolicy.setTimeout(THIRTY_SECONDS);

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(ONE_SECOND);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setThrowLastExceptionOnExhausted(true);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        retryTemplate.execute(rc -> portInUse(dockerHostname, ports[0]));
    }

    private boolean portInUse(String host, int port) {
        LOG.debug("Check whether {}:{} is ready", host, port);
        Socket s = null;
        try {
            s = new Socket(host, port);
            LOG.debug("{}:{} is ready", host, port);
            return true;
        } catch (IOException e) {
            LOG.debug("{}:{} is not ready", host, port);
            throw new EnvironmentBuilderException(format("Container '%s' has failed to start. Port %s:%s not available", containerName, host, port), e);
        } finally {
            IOUtils.closeQuietly(s);
        }
    }

    public Optional<Image> findImage() {
        try {
            List<Image> images = dockerClient.listImages(DockerClient.ListImagesParam.allImages());

            for (Image image : images) {
                if (image.repoTags() != null) {
                    for (String tag : image.repoTags()) {
                        if (tag.equals(imageName)) {
                            return Optional.of(image);
                        }
                    }
                }
            }

        } catch (DockerException | InterruptedException e) {
            LOG.error("Unable to retrieve a list of Docker images", e);
        }
        return Optional.empty();
    }

    public void waitFor(BiConsumer<DockerClient, String> function) {
        TimeoutRetryPolicy retryPolicy = new TimeoutRetryPolicy();
        retryPolicy.setTimeout(THIRTY_SECONDS);

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(ONE_SECOND);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setThrowLastExceptionOnExhausted(true);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        retryTemplate.execute(rc -> { function.accept(dockerClient, containerId); return null; });
    }
}
