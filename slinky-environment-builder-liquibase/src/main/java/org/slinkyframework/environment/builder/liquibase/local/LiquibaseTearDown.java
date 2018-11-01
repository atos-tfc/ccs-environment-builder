package org.slinkyframework.environment.builder.liquibase.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slinkyframework.environment.builder.EnvironmentBuilderException;
import org.slinkyframework.environment.builder.liquibase.LiquibaseBuildDefinition;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseDriver;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseDriverFactory;

import java.sql.SQLException;

public class LiquibaseTearDown {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiquibaseTearDown.class);

    private final String hostname;

    public LiquibaseTearDown(String hostname) {
        this.hostname = hostname;
    }

    public void tearDown(LiquibaseBuildDefinition definition) {

        LOGGER.info("Tearing down database {} on {}", definition.getName(), hostname);

        DatabaseDriver databaseDriver = null;

        try {
            databaseDriver = DatabaseDriverFactory.getInstance(definition);

            databaseDriver.createConnection(hostname);

            databaseDriver.tearDown();

        } catch (SQLException e) {
            throw new EnvironmentBuilderException("Database teardown has failed", e);
        } finally {
            databaseDriver.cleanUp();
        }
    }
}
