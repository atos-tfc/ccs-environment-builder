package org.slinkyframework.environment.builder.liquibase.test.docker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slinkyframework.environment.builder.liquibase.LiquibaseBuildDefinition;
import org.slinkyframework.environment.builder.liquibase.docker.DockerLiquibaseEnvironmentBuilder;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseDriver;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseProperties;
import org.slinkyframework.environment.builder.liquibase.drivers.oracle.OracleProperties;
import org.slinkyframework.environment.builder.liquibase.local.LocalLiquibaseEnvironmentBuilder;
import org.slinkyframework.environment.docker.DockerDriver;

import java.util.TreeSet;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.slinkyframework.environment.docker.test.matchers.HasPortAvailableMatcher.hasPortAvailable;

@RunWith(MockitoJUnitRunner.class)
public class DockerlLiquibaseEnvironmentBuilderIntegrationTest {

    private static final String TEST_HOST = "localvm";
    private static final String TEST_NAME = "TEST_NAME";
    private static final String TEST_USER1 = "TEST_USER1";
    private static final String TEST_CHANGE_LOG = "liquibase/test.db.changelog-master.xml";
    private static final  String TEST_USERNAME = "system";
    private static final String TEST_PASSWORD = "oracle";
    private static final int TEST_PORT = 1521;
    private static final String TEST_SID = "xe";

    private static final String CONTAINER_NAME = "oracle-xe";
    private static final String IMAGE_NAME = "alexeiled/docker-oracle-xe-11g";
    private static final int[] PORTS = { TEST_PORT, 8080};

    @Mock
    private LocalLiquibaseEnvironmentBuilder mockLocalLiquibaseEnvironmentBuilder;

    private DockerLiquibaseEnvironmentBuilder testee;
    private DockerLiquibaseEnvironmentBuilder mockedTestee;

    private DatabaseProperties databaseProperties;
    private DatabaseDriver databaseDriver;
    private DockerDriver dockerDriver;
    private TreeSet<LiquibaseBuildDefinition> buildDefinitions;

    @Before
    public void setUp() {
        testee = new DockerLiquibaseEnvironmentBuilder(new LocalLiquibaseEnvironmentBuilder(TEST_HOST));

        dockerDriver = new DockerDriver(CONTAINER_NAME, IMAGE_NAME, PORTS);

        databaseProperties = new OracleProperties(TEST_USERNAME, TEST_PASSWORD, TEST_PORT, TEST_SID, TEST_USER1);
        LiquibaseBuildDefinition liquibaseBuildDefinition = new LiquibaseBuildDefinition(TEST_NAME, databaseProperties, TEST_CHANGE_LOG);

        buildDefinitions = new TreeSet<>();
        buildDefinitions.add(liquibaseBuildDefinition);

        // Make sure no Docker containers left lying around
        testee.tearDown(buildDefinitions);

        mockedTestee = new DockerLiquibaseEnvironmentBuilder(mockLocalLiquibaseEnvironmentBuilder);

        reset(mockLocalLiquibaseEnvironmentBuilder);
    }

    @After
    public void tearDown() {
        dockerDriver.killAndRemoveContainer();
    }

    @Test
    public void shouldDelegateToLocalLiquibaseEnvironmentBuilderSetUp() {
        mockedTestee.setUp(buildDefinitions);

        verify(mockLocalLiquibaseEnvironmentBuilder).setUp(buildDefinitions);
    }

    @Test
    public void shouldCreateThenStartThenConfigureAContainer() {
        testee.setUp(buildDefinitions);

        assertThat("Container found", dockerDriver.findExistingContainer().isPresent(), is(true));
        assertThat("Port 1521 available", TEST_HOST, hasPortAvailable(1521));
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
