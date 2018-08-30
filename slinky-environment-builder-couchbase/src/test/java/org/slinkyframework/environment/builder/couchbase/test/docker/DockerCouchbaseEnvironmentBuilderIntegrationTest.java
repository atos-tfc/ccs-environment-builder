package org.slinkyframework.environment.builder.couchbase.test.docker;

import com.spotify.docker.client.exceptions.DockerCertificateException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slinkyframework.environment.builder.couchbase.CouchbaseBuildDefinition;
import org.slinkyframework.environment.builder.couchbase.docker.DockerCouchbaseEnvironmentBuilder;
import org.slinkyframework.environment.builder.couchbase.local.LocalCouchbaseEnvironmentBuilder;
import org.slinkyframework.environment.docker.DockerDriver;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.slinkyframework.environment.builder.couchbase.docker.DockerCouchbaseEnvironmentBuilder.CONTAINER_NAME;
import static org.slinkyframework.environment.builder.couchbase.docker.DockerCouchbaseEnvironmentBuilder.COUCHBASE_LATEST_IMAGE_NAME;
import static org.slinkyframework.environment.docker.test.matchers.HasPortAvailableMatcher.hasPortAvailable;

@RunWith(MockitoJUnitRunner.class)
public class DockerCouchbaseEnvironmentBuilderIntegrationTest {

    private static final String TEST_HOST = "dev";
    private static final String TEST_BUCKET_1_NAME = "testBucket1";
    private static final String TEST_BUCKET_1_PASSWORD = "password1";
    private static final String TEST_BUCKET_2_NAME = "testBucker2";
    private static final String TEST_BUCKET_2_PASSWORD = "password2";
    private static final String TEST_DOCUMENT_PACKAGE = "org.example";
    private static final String TEST_DOCUMENT_CLASS_NAME = "ExampleDocument";

    @Mock LocalCouchbaseEnvironmentBuilder mockLocalCouchbaseEnvironmentBuilder;

    private DockerCouchbaseEnvironmentBuilder testee;

    private Set<CouchbaseBuildDefinition> buildDefinitions;
    private DockerDriver dockerDriver;

    @Before
    public void setUp() throws DockerCertificateException {

        testee = new DockerCouchbaseEnvironmentBuilder(mockLocalCouchbaseEnvironmentBuilder);
        buildDefinitions = new TreeSet<>();
        CouchbaseBuildDefinition definition1 = new CouchbaseBuildDefinition("Definition1", TEST_BUCKET_1_NAME, TEST_DOCUMENT_PACKAGE, TEST_DOCUMENT_CLASS_NAME);
        definition1.setBucketPassword(TEST_BUCKET_1_PASSWORD);

        CouchbaseBuildDefinition definition2 = new CouchbaseBuildDefinition("Definition2", TEST_BUCKET_2_NAME, TEST_DOCUMENT_PACKAGE, TEST_DOCUMENT_CLASS_NAME);
        definition2.setBucketPassword(TEST_BUCKET_2_PASSWORD);

        dockerDriver = new DockerDriver(CONTAINER_NAME, COUCHBASE_LATEST_IMAGE_NAME, testee.getInternalToExternalPortsMap());

        // Make sure no Docker containers left lying around
        testee.tearDown(buildDefinitions);

        reset(mockLocalCouchbaseEnvironmentBuilder);
    }

    @Test
    public void shouldDelegateToLocalCouchbaseEnvironmentBuilderSetUp() {
        testee.setUp(buildDefinitions);

        verify(mockLocalCouchbaseEnvironmentBuilder).setUp(buildDefinitions);
    }

    @Test
    public void shouldCallToLocalCouchbaseEnvironmentBuilderTearDown() {
        testee.tearDown(buildDefinitions);

        verify(mockLocalCouchbaseEnvironmentBuilder).tearDown(buildDefinitions);
    }

    @Test
    public void shouldCreateThenStartThenConfigureAContainer() {
        testee.setUp(buildDefinitions);

        assertThat("Container found", dockerDriver.findExistingContainer().isPresent(), is(true));
        assertThat("Port 8091 available", TEST_HOST, hasPortAvailable(8091));
        assertThat("Port 8092 available", TEST_HOST, hasPortAvailable(8092));
        assertThat("Port 8093 available", TEST_HOST, hasPortAvailable(8093));
        assertThat("Port 8094 available", TEST_HOST, hasPortAvailable(8094));
    }

    @Test
    public void shouldSkipCreateAndStartAndJustConfigureARunningContainer() {
        testee.setUp(buildDefinitions);
        testee.setUp(buildDefinitions);

        assertThat("Container found", dockerDriver.findExistingContainer().isPresent(), is(true));
    }

    @Test
    public void shouldTearDownARunningContainer() {
        testee.setUp(buildDefinitions);
        testee.tearDown(buildDefinitions);

        assertThat("Container found", dockerDriver.findExistingContainer().isPresent(), is(false));
    }
}
