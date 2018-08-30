package org.slinkyframework.environment.builder.liquibase.docker;

import org.slinkyframework.environment.builder.EnvironmentBuilder;
import org.slinkyframework.environment.builder.liquibase.LiquibaseBuildDefinition;
import org.slinkyframework.environment.builder.liquibase.local.LocalLiquibaseEnvironmentBuilder;
import org.slinkyframework.environment.docker.DockerDriver;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Creates and starts an Oracle XE Docker container.
 * It then kills and removes the container when environment torn down.
 *
 * NOTE: Assumes the standard Docker Machine environment variables are set:
 *         DOCKER_TLS_VERIFY, DOCKER_HOST, DOCKER_CERT_PATH and DOCKER_MACHINE_NAME
 * Make sure these are set before running IDE or Maven build.
 */
public class DockerLiquibaseEnvironmentBuilder implements EnvironmentBuilder<LiquibaseBuildDefinition>  {

    public static final String CONTAINER_NAME = "oracle-xe";
    public static final String ORACLE_XE_LATEST_IMAGE_NAME = "alexeiled/docker-oracle-xe-11g";

    private final LocalLiquibaseEnvironmentBuilder localEnvironmentBuilder;
    private final Map<Integer, Integer> internalToExternalPortsMap = new HashMap<>();
    private final DockerDriver dockerDriver;

    public DockerLiquibaseEnvironmentBuilder(LocalLiquibaseEnvironmentBuilder localEnvironmentBuilder) {
        this.localEnvironmentBuilder = localEnvironmentBuilder;

        internalToExternalPortsMap.put(1521, 1521);

        dockerDriver = new DockerDriver(CONTAINER_NAME, ORACLE_XE_LATEST_IMAGE_NAME, internalToExternalPortsMap);
    }

    public Map<Integer, Integer> getInternalToExternalPortsMap() {
        return internalToExternalPortsMap;
    }

    @Override
    public void setUp(Set<LiquibaseBuildDefinition> buildDefinitions) {
        dockerDriver.setUpDocker();

        localEnvironmentBuilder.setUp(buildDefinitions);
    }

    @Override
    public void tearDown(Set<LiquibaseBuildDefinition> buildDefinitions) {
        dockerDriver.killAndRemoveContainer();
    }

    @Override
    public void cleanUp() {
        localEnvironmentBuilder.cleanUp();
    }
}
