package org.slinkyframework.environment.builder.liquibase.drivers.oracle;

import org.slinkyframework.environment.builder.liquibase.drivers.TableDoesNotExistException;
import org.slinkyframework.environment.builder.liquibase.drivers.TablespaceDoesNotExistException;
import org.slinkyframework.environment.builder.liquibase.drivers.UserDoesNotExistException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import java.sql.SQLException;

public class OracleSQLExceptionTranslator implements SQLExceptionTranslator {

    public DataAccessException translate(String task, String sql, SQLException e) {
        switch (e.getErrorCode()) {
            case 942:
                return new TableDoesNotExistException("Table does not exist", e);
            case 959:
                return new TablespaceDoesNotExistException("Tablespace does not exist", e);
            case 1918:
                return new UserDoesNotExistException("User does not exist", e);
        }

        return new UncategorizedSQLException("(" + task + "): encountered SQLException [" + e.getMessage() + "]", sql, e);
    }

}