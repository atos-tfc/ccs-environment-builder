package org.slinkyframework.environment.docker.test;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Image;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slinkyframework.environment.builder.EnvironmentBuilderException;
import org.slinkyframework.environment.docker.DockerDriver;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.slinkyframework.environment.docker.test.matchers.HasPortAvailableMatcher.hasPortAvailable;

public class DockerDriverIntegrationTest {

    private static final String CONTAINER_NAME = "tomcat";
    private static final String IMAGE_NAME = "tomcat:8.0";
    private static final int[] PORTS = { 8080 };

    public static final String ENVIRONMENT_DOCKER_HOST = "DOCKER_HOST";
    public static final String ENVIRONMENT_DOCKER_MACHINE_NAME = "DOCKER_MACHINE_NAME";

    private String dockerHost;
    private String dockerMachineName;

    @Before
    public void setUp() {
        dockerHost = System.getenv(ENVIRONMENT_DOCKER_HOST);
        dockerMachineName = System.getenv(ENVIRONMENT_DOCKER_MACHINE_NAME);
    }

    @After
    public void tearDown() throws Exception {
        // Reset environment variable as it might have been changed during a test
        injectEnvironmentVariable(ENVIRONMENT_DOCKER_HOST, dockerHost);
    }

    @Test(expected = EnvironmentBuilderException.class)
    public void shouldNotStartIfEnvironmentVariablesNotSet() throws Exception {
        injectEnvironmentVariable(ENVIRONMENT_DOCKER_HOST, "");

        DockerDriver testee = new DockerDriver(CONTAINER_NAME, IMAGE_NAME, PORTS);
    }

    private static void injectEnvironmentVariable(String key, String value) throws Exception {
        Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");

        Field unmodifiableMapField = getAccessibleField(processEnvironment, "theUnmodifiableEnvironment");
        Object unmodifiableMap = unmodifiableMapField.get(null);
        injectIntoUnmodifiableMap(key, value, unmodifiableMap);

        Field mapField = getAccessibleField(processEnvironment, "theEnvironment");
        Map<String, String> map = (Map<String, String>) mapField.get(null);
        map.put(key, value);
    }

    private static Field getAccessibleField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    private static void injectIntoUnmodifiableMap(String key, String value, Object map) throws ReflectiveOperationException {
        Class unmodifiableMap = Class.forName("java.util.Collections$UnmodifiableMap");
        Field field = getAccessibleField(unmodifiableMap, "m");
        Object obj = field.get(map);
        ((Map<String, String>) obj).put(key, value);
    }

    @Test
    public void shouldStartAContainer() {
        DockerDriver testee = new DockerDriver(CONTAINER_NAME, IMAGE_NAME, PORTS);

        testee.setUpDocker();

        assertThat("Container", testee.findExistingContainer().isPresent(), is(true));
        assertThat(dockerMachineName, hasPortAvailable(PORTS[0]));

    }

    @Test
    public void shouldKillAndRemoveAContainer() {
        DockerDriver testee = new DockerDriver(CONTAINER_NAME, IMAGE_NAME, PORTS);

        testee.setUpDocker();

        testee.killAndRemoveContainer();

        assertThat("Container", testee.findExistingContainer().isPresent(), is(false));
    }

    @Test
    @Ignore("Takes a long time to run. So ignoring for main run.")
    public void shouldPullDownImageIfOneDoesNotExistLocally() throws Exception {
        DockerDriver testee = new DockerDriver(CONTAINER_NAME, IMAGE_NAME, PORTS);
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
