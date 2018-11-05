package org.slinkyframework.environment.builder.liquibase.drivers;

import javax.sql.DataSource;

public interface DatabaseDriver {
    void connect(String hostname);
    void tearDown(String hostname);
    DataSource getDataSource();
}
