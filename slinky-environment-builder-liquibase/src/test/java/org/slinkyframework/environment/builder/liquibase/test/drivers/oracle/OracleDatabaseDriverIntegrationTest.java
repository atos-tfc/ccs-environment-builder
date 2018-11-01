package org.slinkyframework.environment.builder.liquibase.test.drivers.oracle;

import oracle.jdbc.pool.OracleDataSource;
import org.junit.*;
import org.slinkyframework.environment.builder.liquibase.LiquibaseBuildDefinition;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseDriver;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseDriverFactory;
import org.slinkyframework.environment.builder.liquibase.drivers.oracle.OracleDatabaseDriver;
import org.slinkyframework.environment.docker.DockerDriver;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.SQLException;

import static java.lang.String.format;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.slinkyframework.environment.builder.liquibase.test.matchers.OracleUserExistsMatcher.userExists;
import static org.slinkyframework.environment.builder.liquibase.test.utils.TestUtil.createLiquibaseBuildDefinition;
import static org.slinkyframework.environment.builder.liquibase.test.utils.TestUtil.createLiquibaseBuildDefinitionWithTablespace;
import static org.slinkyframework.environment.builder.liquibase.test.utils.TestUtil.startDocker;

public class OracleDatabaseDriverIntegrationTest {

    private static final String TEST_USERNAME = "system";
    private static final int TEST_PORT = 1521;
    private static final String TEST_SID = "XE";
    private static final String TEST_PASSWORD = "oracle";

    private static final String TEST_USER1 = "TEST_USER1";
    private static final String TEST_USER2 = "TEST_USER2";
    private static final String TEST_USER_PASSWORD = "password";

    private static DockerDriver dockerDriver;

    private DatabaseDriver testee;
    private Connection connection;
    private JdbcTemplate jdbcTemplate;

    @BeforeClass
    public static void setUpOnce() {
        dockerDriver = startDocker();
    }

    @AfterClass
    public static void tearDownOnce() {
        dockerDriver.killAndRemoveContainer();
    }

    @Before
    public void setUp() throws SQLException {
        LiquibaseBuildDefinition liquibaseBuildDefinition = createLiquibaseBuildDefinition();

        testee = DatabaseDriverFactory.getInstance(liquibaseBuildDefinition);

        connection = testee.createConnection("localvm");

        jdbcTemplate = new JdbcTemplate(testee.getDataSource());
    }

    @After
    public void tearDown() {
        testee.tearDown();
        testee.cleanUp();
    }

    @Test
    public void shouldCreateInstanceOfOracleDatabaseDriverForOracleProperties() {
        assertThat("DatabaseDriver", testee, instanceOf(OracleDatabaseDriver.class));
    }

    @Test
    public void shouldCreateADatabaseConnection() {
        assertThat("DB Connection", connection, is(notNullValue()));
    }

    @Test
    public void shouldCloseADatabaseConnection() throws SQLException {
        testee.cleanUp();

        assertThat("DB Connection", connection.isClosed(), is(true));
    }

    @Test
    public void shouldTearDownANonExistentUser() throws SQLException {
        testee.tearDown();
    }

    @Test
    public void shouldTearDownAnExistingUsers() throws SQLException {
        createUser(TEST_USER1, TEST_USER_PASSWORD);
        createUser(TEST_USER2, TEST_USER_PASSWORD);

        assertThat(testee.getDataSource(), userExists(TEST_USER1));
        assertThat(testee.getDataSource(), userExists(TEST_USER2));

        testee.tearDown();

        assertThat(testee.getDataSource(), not(userExists(TEST_USER1)));
        assertThat(testee.getDataSource(), not(userExists(TEST_USER2)));
    }

    @Test
    public void shouldTearDownAnExistingUsersWhichHasAnActiveSession() throws SQLException {
        createUser(TEST_USER1, TEST_USER_PASSWORD);

        assertThat(testee.getDataSource(), userExists(TEST_USER1));

        OracleDataSource dataSource = new OracleDataSource();
        dataSource.setUser(TEST_USER1);
        dataSource.setPassword(TEST_USER_PASSWORD);
        dataSource.setURL("jdbc:oracle:thin:@localvm:1521:XE");

        Connection newUserConnection = dataSource.getConnection();

        assertThat("New user connection isClosed", newUserConnection.isClosed(), is(false));

        testee.tearDown();

        assertThat(testee.getDataSource(), not(userExists(TEST_USER1)));
    }

    private void createUser(String username, String password) {
        jdbcTemplate.execute(format("CREATE USER %s IDENTIFIED BY %s DEFAULT TABLESPACE users TEMPORARY TABLESPACE temp", username, password));
        jdbcTemplate.execute(format("GRANT ALL PRIVILEGES TO %s", username));
    }

    @Test
    public void shouldTearDownANonExistentTablespace() throws SQLException {
        LiquibaseBuildDefinition liquibaseBuildDefinition = createLiquibaseBuildDefinitionWithTablespace("unknown");

        testee = DatabaseDriverFactory.getInstance(liquibaseBuildDefinition);

        connection = testee.createConnection("localvm");
        jdbcTemplate = new JdbcTemplate(testee.getDataSource());

        testee.tearDown();
    }
}
