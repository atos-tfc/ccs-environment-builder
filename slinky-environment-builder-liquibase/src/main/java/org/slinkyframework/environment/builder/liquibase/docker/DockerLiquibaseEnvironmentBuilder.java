package org.slinkyframework.environment.builder.liquibase.docker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slinkyframework.environment.builder.EnvironmentBuilder;
import org.slinkyframework.environment.builder.EnvironmentBuilderException;
import org.slinkyframework.environment.builder.liquibase.LiquibaseBuildDefinition;
import org.slinkyframework.environment.builder.liquibase.local.LocalLiquibaseEnvironmentBuilder;
import org.slinkyframework.environment.docker.DockerDriver;

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

    private static final Logger LOG = LoggerFactory.getLogger(DockerLiquibaseEnvironmentBuilder.class);

    public static final String CONTAINER_NAME = "oracle-xe";
    public static final String ORACLE_XE_LATEST_IMAGE_NAME = "alexeiled/docker-oracle-xe-11g";
    public  static final int[] ORACLE_XE_PORTS = { 1521 };

    private final LocalLiquibaseEnvironmentBuilder localEnvironmentBuilder;
    private final DockerDriver dockerDriver;

    public DockerLiquibaseEnvironmentBuilder(LocalLiquibaseEnvironmentBuilder localEnvironmentBuilder) {
        this.localEnvironmentBuilder = localEnvironmentBuilder;

        dockerDriver = new DockerDriver(CONTAINER_NAME, ORACLE_XE_LATEST_IMAGE_NAME, ORACLE_XE_PORTS);
    }

    @Override
    public void setUp(Set<LiquibaseBuildDefinition> buildDefinitions) {
        dockerDriver.setUpDocker();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new EnvironmentBuilderException("Exception waiting for Oracle XE container to start", e);
        }

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
