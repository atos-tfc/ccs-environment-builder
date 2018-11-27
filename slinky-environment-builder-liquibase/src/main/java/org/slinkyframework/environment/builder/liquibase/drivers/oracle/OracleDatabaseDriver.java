package org.slinkyframework.environment.builder.liquibase.drivers.oracle;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slinkyframework.environment.builder.EnvironmentBuilderException;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseDriver;
import org.slinkyframework.environment.builder.liquibase.drivers.TableDoesNotExistException;
import org.slinkyframework.environment.builder.liquibase.drivers.TablespaceDoesNotExistException;
import org.slinkyframework.environment.builder.liquibase.drivers.UserDoesNotExistException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.List;

import static java.lang.String.format;

public class OracleDatabaseDriver implements DatabaseDriver {

    private static final Logger LOGGER = LoggerFactory.getLogger(OracleDatabaseDriver.class);

    private static final String DATABASECHANGELOG_SQL = "delete from DATABASECHANGELOG where ID like ?";

    private static final int ONE_SECOND = 1000;
    private static final long THIRTY_SECONDS = 30000;
    private static final long TEN_SECONDS = 10000;

    private OracleProperties properties;
    private String changeLogPrefix;
    private HikariDataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    public OracleDatabaseDriver(OracleProperties properties, String changeLogPrefix) {
        this.properties = properties;
        this.changeLogPrefix = changeLogPrefix;
    }

    @Override
    public void connect(String hostname) {

        if (dataSource == null) {
            createDataSource(hostname);
            createJdbcTemplate();
        }
    }


    private void createDataSource(String hostname) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl(hostname));
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(5);
        config.setInitializationFailTimeout(THIRTY_SECONDS);

        dataSource = new HikariDataSource(config);
    }

    private void createJdbcTemplate() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setExceptionTranslator(new OracleSQLExceptionTranslator());
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void tearDown(String hostname) {
        connect(hostname);

        properties.getUsers().forEach(this::dropUser);
        properties.getUsers().forEach(this::dropPublicSynonyms);
        properties.getTablespaces().forEach(this::dropTablespace);

        cleanUpDatabaseChangeLog();
    }

    private void dropUser(String username) {
        if (jdbcTemplate != null) {
            try {
                killSessions(username);
            } catch (EnvironmentBuilderException e) {
                LOGGER.warn("Session should be killed by now");
            }

            LOGGER.info("Dropping database user '{}'", username);

            try {
                jdbcTemplate.execute(format("DROP USER %s CASCADE", username));
            } catch (UserDoesNotExistException e) {
                LOGGER.debug("Unable to drop user '{}'. User does not exist.", username);
            }
        }
    }

    private void dropPublicSynonyms(String username) {
        LOGGER.info("Dropping public synonyms user '{}'", username);

        findPublicSynonymsForUser(username).stream()
                .forEach(this::dropPublicSynonym);
    }

    private List<String> findPublicSynonymsForUser(String username) {
        verifyConnected();

        return jdbcTemplate.queryForList("select SYNONYM_NAME from ALL_SYNONYMS where TABLE_OWNER like ?", String.class, username);
    }

    private void verifyConnected() {
        if (jdbcTemplate == null) {
            throw new EnvironmentBuilderException("OracleDatabaseDriver not connected. Please call createConnection() first.");
        }
    }

    private void dropPublicSynonym(String synonymName) {
        LOGGER.info("Dropping public synonyms '{}'", synonymName);

        verifyConnected();

        try {
            jdbcTemplate.execute(format("DROP PUBLIC SYNONYM %s", synonymName));
        } catch (UserDoesNotExistException e) {
            LOGGER.debug("Unable to drop public synonym '{}'. {}", synonymName, e.getMessage());
        }
    }

    private void dropTablespace(String tablespace) {
        LOGGER.info("Dropping database tablespace '{}'", tablespace);

        verifyConnected();

        try {
            jdbcTemplate.execute(format("DROP TABLESPACE %s INCLUDING CONTENTS AND DATAFILES", tablespace));
        } catch (TablespaceDoesNotExistException e) {
            LOGGER.debug("Unable to drop tablespace '{}'. Tablespace does not exist.", tablespace);
        }
    }

    private void killSessions(String username) {
        processSessions(username, this::killSessions);

        waitForSessionsToDie(username);
    }

    private void processSessions(String username, RowCallbackHandler f) {
        Object[] args = { username };

        verifyConnected();

        jdbcTemplate.query("SELECT username, sid, serial# FROM v$session WHERE username = UPPER(?)", args, f);
    }

    private void killSessions(ResultSet rs) {
        try {
            String username = rs.getString(1);
            String sid = rs.getString(2);
            String serial = rs.getString(3);

            LOGGER.info("Killing database session for USERNAME '{}', SID '{}' and SERIAL '{}'", username, sid, serial);

            verifyConnected();

            jdbcTemplate.update(format("ALTER SYSTEM KILL SESSION '%s,%s' IMMEDIATE", sid, serial));

        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }

    private void waitForSessionsToDie(String username) {
        LOGGER.debug("Waiting for database sessions to die '{}'", username);

        TimeoutRetryPolicy retryPolicy = new TimeoutRetryPolicy();
        retryPolicy.setTimeout(TEN_SECONDS);

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(ONE_SECOND);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setThrowLastExceptionOnExhausted(true);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        retryTemplate.execute(rc -> { processSessions(username, this::throwExceptionIfSessionExists); return null; });
    }

    private void throwExceptionIfSessionExists(ResultSet resultSet) {
        throw new EnvironmentBuilderException("Session has not been killed");
    }

    private void cleanUpDatabaseChangeLog() {
        String parameter = changeLogPrefix + "%";

        LOGGER.info("Cleaning up DATABASECHANGELOG table for ID like '{}'", parameter);

        verifyConnected();

        try {
            jdbcTemplate.update(DATABASECHANGELOG_SQL, parameter);
        } catch (TableDoesNotExistException e) {
            LOGGER.debug("Unable to cleanup DATABASECHANGELOG. Table does not exist.");
        }
    }
}
