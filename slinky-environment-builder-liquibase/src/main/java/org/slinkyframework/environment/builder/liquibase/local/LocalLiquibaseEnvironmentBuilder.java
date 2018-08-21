package org.slinkyframework.environment.builder.liquibase.local;

import org.slinkyframework.environment.builder.EnvironmentBuilder;
import org.slinkyframework.environment.builder.liquibase.LiquibaseBuildDefinition;

import java.util.Set;

public class LocalLiquibaseEnvironmentBuilder implements EnvironmentBuilder<LiquibaseBuildDefinition> {

    private final String hostname;
    private LiquibaseSetUp liquibaseSetUp;
    private LiquibaseTearDown liquibaseTearDown;

    public LocalLiquibaseEnvironmentBuilder(String hostname) {
        this.hostname = hostname;
        liquibaseSetUp = new LiquibaseSetUp(hostname);
        liquibaseTearDown = new LiquibaseTearDown(hostname);
    }

    @Override
    public void setUp(Set<LiquibaseBuildDefinition> buildDefinitions) {
        buildDefinitions.forEach(definition -> liquibaseSetUp.setUp(definition));
    }

    @Override
    public void tearDown(Set<LiquibaseBuildDefinition> buildDefinitions) {
        buildDefinitions.forEach(definition -> liquibaseTearDown.tearDown(definition));
    }

    @Override
    public void cleanUp() {

    }

    public String getHostname() {
        return hostname;
    }
}
