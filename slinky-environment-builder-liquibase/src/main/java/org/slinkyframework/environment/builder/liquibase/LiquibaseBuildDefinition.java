package org.slinkyframework.environment.builder.liquibase;

import org.slinkyframework.environment.builder.definition.AbstractBuildDefinition;
import org.slinkyframework.environment.builder.definition.BuildPriority;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseProperties;

public class LiquibaseBuildDefinition extends AbstractBuildDefinition {

    private final DatabaseProperties databaseProperties;
    private final String changeLogPrefix;
    private final String changeLogFile;

    public LiquibaseBuildDefinition(String name, DatabaseProperties databaseProperties,
                                    String changeLogPrefix, String changeLogFile) {
        super(name);
        this.databaseProperties = databaseProperties;
        this.changeLogPrefix = changeLogPrefix;
        this.changeLogFile = changeLogFile;
    }

    public LiquibaseBuildDefinition(
            BuildPriority priority, String name, DatabaseProperties databaseProperties,
            String changeLogPrefix, String changeLogFile) {
        super(priority, name);
        this.databaseProperties = databaseProperties;
        this.changeLogPrefix = changeLogPrefix;
        this.changeLogFile = changeLogFile;
    }

    public DatabaseProperties getDatabaseProperties() {
        return databaseProperties;
    }

    public String getChangeLogPrefix() {
        return changeLogPrefix;
    }

    public String getChangeLogFile() {
        return changeLogFile;
    }
}
