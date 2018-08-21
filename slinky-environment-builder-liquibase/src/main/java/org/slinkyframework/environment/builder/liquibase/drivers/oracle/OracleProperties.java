package org.slinkyframework.environment.builder.liquibase.drivers.oracle;

import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseProperties;

import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

public class OracleProperties implements DatabaseProperties {

    private String username;
    private String password;
    private int port;
    private String sid;
    private List<String> users;

    public OracleProperties(String username, String password, int port, String sid, String... users) {
        this.username = username;
        this.password = password;
        this.port = port;
        this.sid = sid;
        this.users = Arrays.asList(users);
    }

    String getUsername() {
        return username;
    }

    String getPassword() {
        return password;
    }

    List<String> getUsers() {
        return users;
    }

    String getUrl(String hostname) {
        return format("jdbc:oracle:thin:@%s:%s:%s", hostname, port, sid);
    }
}
