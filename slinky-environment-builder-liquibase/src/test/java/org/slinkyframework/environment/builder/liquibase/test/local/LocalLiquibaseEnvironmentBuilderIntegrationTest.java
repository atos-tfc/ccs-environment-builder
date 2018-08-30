package org.slinkyframework.environment.builder.liquibase.test.local;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slinkyframework.environment.builder.liquibase.LiquibaseBuildDefinition;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseDriver;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseDriverFactory;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseProperties;
import org.slinkyframework.environment.builder.liquibase.drivers.oracle.OracleProperties;
import org.slinkyframework.environment.builder.liquibase.local.LocalLiquibaseEnvironmentBuilder;
import org.slinkyframework.environment.docker.DockerDriver;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertThat;
import static org.slinkyframework.environment.builder.liquibase.docker.DockerLiquibaseEnvironmentBuilder.CONTAINER_NAME;
import static org.slinkyframework.environment.builder.liquibase.docker.DockerLiquibaseEnvironmentBuilder.ORACLE_XE_LATEST_IMAGE_NAME;
import static org.slinkyframework.environment.builder.liquibase.test.matchers.OracleUserExistsMatcher.userExists;

public class LocalLiquibaseEnvironmentBuilderIntegrationTest {

    private static final String TEST_HOST = "localvm";
    private static final String TEST_NAME = "TEST_NAME";
    private static final String TEST_USER1 = "TEST_USER1";
    private static final String TEST_CHANGE_LOG = "liquibase/test.db.changelog-master.xml";
    private static final  String TEST_USERNAME = "system";
    private static final String TEST_PASSWORD = "oracle";
    private static final int TEST_PORT = 1521;
    private static final String TEST_SID = "xe";

    private static DockerDriver dockerDriver;

    private LocalLiquibaseEnvironmentBuilder testee = new LocalLiquibaseEnvironmentBuilder(TEST_HOST);
    private DatabaseProperties databaseProperties;
    private DatabaseDriver databaseDriver;

    @BeforeClass
    public static void setUpOnce() throws InterruptedException {
        Map<Integer, Integer> internalToExternalPortsMap = new HashMap<>();
        internalToExternalPortsMap.put(1521, 1521);

        dockerDriver = new DockerDriver(CONTAINER_NAME, ORACLE_XE_LATEST_IMAGE_NAME, internalToExternalPortsMap);
        dockerDriver.setUpDocker();

        Thread.sleep(10000);
    }

    @AfterClass
    public static void tearDownOnce() {
        dockerDriver.killAndRemoveContainer();
    }

    @Before
    public void setUp() {
        databaseProperties = new OracleProperties(TEST_USERNAME, TEST_PASSWORD, TEST_PORT, TEST_SID, TEST_USER1);
        databaseDriver = DatabaseDriverFactory.getInstance(databaseProperties);
    }
    
    @Test
    public void shouldTearDownANonExistentUser() {
        DatabaseProperties unknowUserProperties = new OracleProperties(TEST_USERNAME, TEST_PASSWORD, TEST_PORT, TEST_SID, "UNKNOWN");

        LiquibaseBuildDefinition liquibaseBuildDefinition = new LiquibaseBuildDefinition(TEST_NAME, unknowUserProperties, TEST_CHANGE_LOG);

        testee.tearDown(toSet(liquibaseBuildDefinition));

        // Success - no exception thrown
    }

    private Set<LiquibaseBuildDefinition> toSet(LiquibaseBuildDefinition liquibaseBuildDefinition) {
        Set<LiquibaseBuildDefinition> buildDefinitions = new HashSet<>();
        buildDefinitions.add(liquibaseBuildDefinition);

        return buildDefinitions;
    }

    @Test
    public void shouldSetUpUserFromLiquibase() throws SQLException {
        LiquibaseBuildDefinition liquibaseBuildDefinition = new LiquibaseBuildDefinition(TEST_NAME, databaseProperties, TEST_CHANGE_LOG);

        testee.setUp(toSet(liquibaseBuildDefinition));

        databaseDriver.createConnection(TEST_HOST);
        assertThat(databaseDriver.getDataSource(), userExists(TEST_USER1));
    }

    @Test
    public void shouldTearDownAUser() {
        DatabaseProperties unknowUserProperties = new OracleProperties(TEST_USERNAME, TEST_PASSWORD, TEST_PORT, TEST_SID, "UNKNOWN");

        LiquibaseBuildDefinition liquibaseBuildDefinition = new LiquibaseBuildDefinition(TEST_NAME, unknowUserProperties, TEST_CHANGE_LOG);

        testee.tearDown(toSet(liquibaseBuildDefinition));

        // Success - no exception thrown
    }
}
