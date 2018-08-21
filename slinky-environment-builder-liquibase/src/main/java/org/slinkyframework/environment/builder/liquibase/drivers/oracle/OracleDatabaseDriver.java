package org.slinkyframework.environment.builder.liquibase.drivers.oracle;

import oracle.jdbc.pool.OracleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseDriver;
import org.slinkyframework.environment.builder.liquibase.drivers.UserDoesNotExistException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.lang.String.format;

public class OracleDatabaseDriver implements DatabaseDriver {

    private static final Logger LOGGER = LoggerFactory.getLogger(OracleDatabaseDriver.class);

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

            LOGGER.debug("Connecting to database '{}'", dataSource.getURL());

            connection = dataSource.getConnection();

            jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.setExceptionTranslator(new OracleSQLExceptionTranslator());
        }

        return connection;
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
    }

    private void dropUser(String username) {
        if (connection != null && jdbcTemplate != null) {
            killSessions(username);

            LOGGER.info("Dropping database user '{}'", username);

            try {
                jdbcTemplate.execute(format("DROP USER %s CASCADE", username));
            } catch (UserDoesNotExistException e) {
                LOGGER.debug("Unable to drop user '{}'. Uesr does not exist.");
            }
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
