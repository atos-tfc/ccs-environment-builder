package org.slinkyframework.environment.builder.liquibase.drivers;

import org.springframework.dao.DataAccessException;

import java.sql.SQLException;

public class TablespaceDoesNotExistException extends DataAccessException {
    public TablespaceDoesNotExistException(String message, SQLException e) {
        super(message, e);
    }
}
