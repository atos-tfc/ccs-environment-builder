package org.slinkyframework.environment.builder.liquibase;

import org.apache.commons.lang3.NotImplementedException;
import org.slinkyframework.environment.builder.EnvironmentBuilder;
import org.slinkyframework.environment.builder.EnvironmentBuilderContext;
import org.slinkyframework.environment.builder.factory.EnvironmentBuilderFactory;
import org.slinkyframework.environment.builder.liquibase.docker.DockerLiquibaseEnvironmentBuilder;
import org.slinkyframework.environment.builder.liquibase.local.LocalLiquibaseEnvironmentBuilder;
import org.springframework.stereotype.Component;

@Component
public class LiquibaseEnvironmentBuilderFactory implements EnvironmentBuilderFactory {

    @Override
    public boolean forClass(Class buildDefinitionClass) {
        return buildDefinitionClass.equals(LiquibaseBuildDefinition.class);
    }

    @Override
    public EnvironmentBuilder getInstance(EnvironmentBuilderContext environmentBuilderContext) {
        LocalLiquibaseEnvironmentBuilder localLiquibaseEnvironmentBuilder
                = new LocalLiquibaseEnvironmentBuilder(environmentBuilderContext.getTargetHost());

        if (environmentBuilderContext.isUseDocker()) {
            return new DockerLiquibaseEnvironmentBuilder(localLiquibaseEnvironmentBuilder);
        } else {
            return localLiquibaseEnvironmentBuilder;
        }
    }
}
