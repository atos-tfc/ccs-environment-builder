package org.slinkyframework.environment.builder.liquibase.test.matchers;

import org.assertj.core.util.Arrays;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

import static java.lang.String.format;

public class OracleUserExistsMatcher extends TypeSafeMatcher<DataSource> {

    private String username;

    public OracleUserExistsMatcher(String username) {
        this.username = username;
    }

    @Override
    protected boolean matchesSafely(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ALL_USERS WHERE USERNAME=UPPER(?)", Arrays.array(username), Integer.class);

        return count == 1 ? true : false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(format("User '%s' to exist", username));
    }

    @Override
    protected void describeMismatchSafely(DataSource dataSource, Description mismatchDescription) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        List<String> users = jdbcTemplate.queryForList("SELECT USERNAME FROM ALL_USERS ORDER BY USERNAME", String.class);

        mismatchDescription.appendText("Users actually found");
        mismatchDescription.appendValue(users.toArray());
    }

    public static OracleUserExistsMatcher userExists(String username) {
        return new OracleUserExistsMatcher(username);
    }
}
