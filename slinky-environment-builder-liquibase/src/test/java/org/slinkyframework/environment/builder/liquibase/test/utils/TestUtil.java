package org.slinkyframework.environment.builder.liquibase.test.utils;

import org.slinkyframework.environment.builder.liquibase.LiquibaseBuildDefinition;
import org.slinkyframework.environment.builder.liquibase.drivers.oracle.OracleProperties;
import org.slinkyframework.environment.docker.DockerDriver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.slinkyframework.environment.builder.liquibase.docker.DockerLiquibaseEnvironmentBuilder.CONTAINER_NAME;
import static org.slinkyframework.environment.builder.liquibase.docker.DockerLiquibaseEnvironmentBuilder.ORACLE_XE_LATEST_IMAGE_NAME;

public class TestUtil {

    public static final String TEST_HOST = "localvm";
    public static final String TEST_NAME = "TEST_NAME";
    public static final String TEST_CHANGE_LOG_PREFIX = "test";
    public static final String TEST_CHANGE_LOG = "liquibase/test.db.changelog-master.xml";

    public static final String TEST_USERNAME = "system";
    public static final int TEST_PORT = 1521;
    public static final String TEST_PDB = "XEPDB1";
    public static final String TEST_PASSWORD = "oracle";
    public static final String TEST_USER1 = "TEST_USER1";
    public static final String TEST_USER2 = "TEST_USER2";

    public static final String TEST_USER_PASSWORD = "password";

    public static final DockerDriver startDocker()
    {
        Map<Integer, Integer> internalToExternalPortsMap = new HashMap<>();
        internalToExternalPortsMap.put(1521, 1521);

        DockerDriver dockerDriver = new DockerDriver(CONTAINER_NAME, ORACLE_XE_LATEST_IMAGE_NAME, internalToExternalPortsMap);
        dockerDriver.setUpDocker();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return dockerDriver;
    }

    public static final LiquibaseBuildDefinition createLiquibaseBuildDefinition() {
        return new LiquibaseBuildDefinition(TEST_NAME, createOracleProperties(), TEST_CHANGE_LOG_PREFIX, TEST_CHANGE_LOG);
    }

    public static final LiquibaseBuildDefinition createLiquibaseBuildDefinitionWithUser(String user) {
        return new LiquibaseBuildDefinition(TEST_NAME, createOraclePropertiesWithUser(user), TEST_CHANGE_LOG_PREFIX, TEST_CHANGE_LOG);
    }
    public static final LiquibaseBuildDefinition createLiquibaseBuildDefinitionWithTablespace(String tablespace) {
        return new LiquibaseBuildDefinition(TEST_NAME, createOraclePropertiesWithTablespace(tablespace), TEST_CHANGE_LOG_PREFIX, TEST_CHANGE_LOG);
    }
    public static final OracleProperties createOraclePropertiesWithTablespace(String tablespace) {
        return new OracleProperties(TEST_USERNAME, TEST_PASSWORD, TEST_PORT, TEST_PDB, Arrays.asList(TEST_USER1, TEST_USER2), Arrays.asList(tablespace));
    }
    public static final OracleProperties createOraclePropertiesWithUser(String user) {
        return new OracleProperties(TEST_USERNAME, TEST_PASSWORD, TEST_PORT, TEST_PDB, Arrays.asList(user));
    }
    public static final OracleProperties createOracleProperties() {
        return new OracleProperties(TEST_USERNAME, TEST_PASSWORD, TEST_PORT, TEST_PDB, Arrays.asList(TEST_USER1, TEST_USER2));
    }
}
