package org.slinkyframework.environment.builder.liquibase.drivers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseDriver {
    Connection createConnection(String hostname) throws SQLException;
    DataSource getDataSource();

    void cleanUp();
    void tearDown();

}
