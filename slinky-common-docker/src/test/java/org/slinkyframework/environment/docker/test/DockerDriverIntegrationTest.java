package org.slinkyframework.environment.docker.test;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Image;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slinkyframework.environment.docker.DockerDriver;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.slinkyframework.environment.docker.PortSelector.selectFreePort;
import static org.slinkyframework.environment.docker.test.matchers.HasPortAvailableMatcher.hasPortAvailable;

public class DockerDriverIntegrationTest {

    private static final String CONTAINER_NAME = "tomcat";
    private static final String IMAGE_NAME = "tomcat:8.0";
    private static final int TOMCAT_INTERNAL_PORT = 8080;

    private static final String ENVIRONMENT_DOCKER_MACHINE_NAME = "DOCKER_MACHINE_NAME";
    private static final String DEFAULT_DOCKER_MACHINE_NAME = "localhost";

    private String dockerMachineName = "dev";

    @Before
    public void setUp() {
        dockerMachineName = System.getenv(ENVIRONMENT_DOCKER_MACHINE_NAME);

        if (dockerMachineName == null) {
            dockerMachineName = DEFAULT_DOCKER_MACHINE_NAME;
        }
    }

    @Test
    public void shouldStartAContainer() {
        Map<Integer, Integer> ports = createPortsMap();
        DockerDriver testee = new DockerDriver(CONTAINER_NAME, IMAGE_NAME, ports);

        testee.setUpDocker();

        assertThat("Container", testee.findExistingContainer().isPresent(), is(true));
        assertThat(dockerMachineName, hasPortAvailable(ports.get(TOMCAT_INTERNAL_PORT)));

    }

    private Map<Integer, Integer> createPortsMap() {
        Map<Integer, Integer> ports = new HashMap<>();
        ports.put(TOMCAT_INTERNAL_PORT, selectFreePort());

        return ports;
    }

    @Test
    public void shouldKillAndRemoveAContainer() {
        Map<Integer, Integer> ports = createPortsMap();
        DockerDriver testee = new DockerDriver(CONTAINER_NAME, IMAGE_NAME, ports);

        testee.setUpDocker();

        testee.killAndRemoveContainer();

        assertThat("Container", testee.findExistingContainer().isPresent(), is(false));
    }

    @Test
    @Ignore("Takes a long time to run. So ignoring for main run.")
    public void shouldPullDownImageIfOneDoesNotExistLocally() throws Exception {
        Map<Integer, Integer> ports = createPortsMap();
        DockerDriver testee = new DockerDriver(CONTAINER_NAME, IMAGE_NAME, ports);
        Optional<Image> image = testee.findImage();

        removeExistingImage(image);

        testee.setUpDocker();

        assertThat("Container found", testee.findExistingContainer().isPresent(), is(true));
    }

    private void removeExistingImage(Optional<Image> image) throws DockerException, InterruptedException, DockerCertificateException {
        DockerClient dockerClient = DefaultDockerClient.fromEnv().build();

        if (image.isPresent()) {
            boolean force = true;
            boolean noPrune = false;

            dockerClient.removeImage(image.get().id(), force, noPrune);
        }
    }
}
