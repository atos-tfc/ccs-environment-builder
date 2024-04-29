package org.slinkyframework.environment.builder.liquibase.drivers.oracle;

import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseProperties;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

public class OracleProperties implements DatabaseProperties {

    private String username;
    private String password;
    private int port;
    private String pdb;
    private List<String> users;
    private List<String> tablespaces;

    public OracleProperties(String username, String password, int port, String pdb, String user) {
        this(username, password, port, pdb, Arrays.asList(user));
    }

    public OracleProperties(String username, String password, int port, String pdb, List<String> users) {
        this(username, password, port, pdb, users, Collections.EMPTY_LIST);
    }

    public OracleProperties(String username, String password, int port, String pdb, List<String> users, List<String> tablespaces) {
        this.username = username;
        this.password = password;
        this.port = port;
        this.pdb = pdb;
        this.users = users;
        this.tablespaces = tablespaces;
    }

    String getUsername() {
        return username;
    }

    String getPassword() {
        return password;
    }

    public List<String> getTablespaces() {
        return tablespaces;
    }

    List<String> getUsers() {
        return users;
    }

    String getUrl(String hostname) {
        return format("jdbc:oracle:thin:@%s:%s/%s", hostname, port, pdb);
    }
}
