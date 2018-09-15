package org.slinkyframework.environment.builder.liquibase.drivers.oracle;

import oracle.jdbc.pool.OracleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseDriver;
import org.slinkyframework.environment.builder.liquibase.drivers.UserDoesNotExistException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static java.lang.String.format;

public class OracleDatabaseDriver implements DatabaseDriver {

    private static final Logger LOGGER = LoggerFactory.getLogger(OracleDatabaseDriver.class);

    private static final int ONE_SECOND = 1000;
    private static final long THIRTY_SECONDS = 30000;

    private OracleProperties properties;
    private Connection connection;
    private OracleDataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    public OracleDatabaseDriver(OracleProperties properties) {
        this.properties = properties;
    }

    @Override
    public Connection createConnection(String hostname) throws SQLException {

        if (connection == null) {
            dataSource = new OracleDataSource();
            dataSource.setURL(properties.getUrl(hostname));
            dataSource.setUser(properties.getUsername());
            dataSource.setPassword(properties.getPassword());

            waitForConnection(dataSource);

            jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.setExceptionTranslator(new OracleSQLExceptionTranslator());
        }

        return connection;
    }

    private void waitForConnection(OracleDataSource dataSource) throws SQLException {
        LOGGER.debug("Connecting to database '{}'", dataSource.getURL());

        TimeoutRetryPolicy retryPolicy = new TimeoutRetryPolicy();
        retryPolicy.setTimeout(THIRTY_SECONDS);

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(ONE_SECOND);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setThrowLastExceptionOnExhausted(true);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        retryTemplate.execute(rc -> connection = dataSource.getConnection());
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void cleanUp() {

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.debug("Problem closing database connection", e);

            }
        }
    }

    @Override
    public void tearDown() {
        properties.getUsers().forEach(this::dropUser);
        properties.getUsers().forEach(this::dropPublicSynonyms);
        properties.getTablespaces().forEach(this::dropTablespace);
    }

    private void dropUser(String username) {
        if (connection != null && jdbcTemplate != null) {
            killSessions(username);

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
        return jdbcTemplate.queryForList("select SYNONYM_NAME from ALL_SYNONYMS where TABLE_OWNER like ?", String.class, username);
    }

    private void dropPublicSynonym(String synonymName) {
        LOGGER.info("Dropping public synonyms '{}'", synonymName);
        try {
            jdbcTemplate.execute(format("DROP PUBLIC SYNONYM %s", synonymName));
        } catch (UserDoesNotExistException e) {
            LOGGER.debug("Unable to drop public synonym '{}'. {}", synonymName, e.getMessage());
        }
    }

    private void dropTablespace(String tablespace) {
        LOGGER.info("Dropping database tablespace '{}'", tablespace);

        try {
            jdbcTemplate.execute(format("DROP TABLESPACE %s INCLUDING CONTENTS AND DATAFILES", tablespace));
        } catch (Exception e) {
            LOGGER.error("Unable to drop tablespace '{}'. {}", tablespace, e.getMessage(), e);
            throw e;
        }
    }

    private void killSessions(String username) {
        Object[] args = { username };

        jdbcTemplate.query("SELECT sid, serial# FROM v$session WHERE username = UPPER(?)", args, new RowCallbackHandler() {

            @Override
            public void processRow(ResultSet rs) {
                try {
                    String sid      = rs.getString(1);
                    String serial   = rs.getString(2);

                    LOGGER.info("Killing database session SID '{}' and SERIAL '{}'", sid, serial);

                    jdbcTemplate.update(format("ALTER SYSTEM KILL SESSION '%s,%s' ", sid, serial));
                } catch (Exception e) {
                    LOGGER.debug(e.getMessage(), e);
                }
            }
        });
    }
}
