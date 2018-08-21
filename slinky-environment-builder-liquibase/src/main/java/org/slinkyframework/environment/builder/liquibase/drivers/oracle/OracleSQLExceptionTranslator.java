package org.slinkyframework.environment.builder.liquibase.drivers.oracle;

import org.slinkyframework.environment.builder.liquibase.drivers.UserDoesNotExistException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import java.sql.SQLException;

public class OracleSQLExceptionTranslator implements SQLExceptionTranslator {

    public DataAccessException translate(String task, String sql, SQLException e) {
        switch (e.getErrorCode()) {
            case 1918:
                return new UserDoesNotExistException("User does not exist", e);
        }

        return new UncategorizedSQLException("(" + task + "): encountered SQLException [" + e.getMessage() + "]", sql, e);
    }

}