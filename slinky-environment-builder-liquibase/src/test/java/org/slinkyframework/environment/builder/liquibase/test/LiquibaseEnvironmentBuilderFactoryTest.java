package org.slinkyframework.environment.builder.liquibase.test;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.slinkyframework.environment.builder.EnvironmentBuilder;
import org.slinkyframework.environment.builder.EnvironmentBuilderContext;
import org.slinkyframework.environment.builder.liquibase.LiquibaseEnvironmentBuilderFactory;
import org.slinkyframework.environment.builder.liquibase.docker.DockerLiquibaseEnvironmentBuilder;
import org.slinkyframework.environment.builder.liquibase.local.LocalLiquibaseEnvironmentBuilder;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LiquibaseEnvironmentBuilderFactoryTest {

    private static final String TEST_HOST = "test";

    private LiquibaseEnvironmentBuilderFactory testee;

    @Before
    public void setUp() {
        testee = new LiquibaseEnvironmentBuilderFactory();

    }

    @Test
    public void shouldGetEnvironmentBuilderToBuildLocally() {
        boolean useDocker = false;
        EnvironmentBuilderContext context = new EnvironmentBuilderContext(TEST_HOST, useDocker);

        EnvironmentBuilder environmentBuilder = testee.getInstance(context);

        assertThat("LiquibaseEnvironmentBuilder", environmentBuilder, is(instanceOf(LocalLiquibaseEnvironmentBuilder.class)));
    }

    @Test
    public void shouldGetEnvironmentBuilderToBuildInDocker() {
        boolean useDocker = true;
        EnvironmentBuilderContext context = new EnvironmentBuilderContext(TEST_HOST, useDocker);

        EnvironmentBuilder environmentBuilder = testee.getInstance(context);

        assertThat("DockerLiquibaseEnvironmentBuilder", environmentBuilder, is(instanceOf(DockerLiquibaseEnvironmentBuilder.class)));
    }
}
