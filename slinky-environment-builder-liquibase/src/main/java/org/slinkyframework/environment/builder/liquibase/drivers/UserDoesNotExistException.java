package org.slinkyframework.environment.builder.liquibase.drivers;

import org.springframework.dao.DataAccessException;

import java.sql.SQLException;

public class UserDoesNotExistException extends DataAccessException {
    public UserDoesNotExistException(String message, SQLException e) {
        super(message, e);
    }
}
