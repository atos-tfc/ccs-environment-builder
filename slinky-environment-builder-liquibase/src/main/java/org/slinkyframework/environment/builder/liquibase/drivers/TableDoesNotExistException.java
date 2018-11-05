package org.slinkyframework.environment.builder.liquibase.drivers;

import org.springframework.dao.DataAccessException;

import java.sql.SQLException;

public class TableDoesNotExistException extends DataAccessException {
    public TableDoesNotExistException(String message, SQLException e) {
        super(message, e);
    }
}
