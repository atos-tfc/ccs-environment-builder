package org.slinkyframework.environment.builder.liquibase.test.drivers.oracle;

import oracle.jdbc.pool.OracleDataSource;
import org.junit.*;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseDriver;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseDriverFactory;
import org.slinkyframework.environment.builder.liquibase.drivers.oracle.OracleDatabaseDriver;
import org.slinkyframework.environment.builder.liquibase.drivers.oracle.OracleProperties;
import org.slinkyframework.environment.docker.DockerDriver;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.slinkyframework.environment.builder.liquibase.docker.DockerLiquibaseEnvironmentBuilder.*;
import static org.slinkyframework.environment.builder.liquibase.test.matchers.OracleUserExistsMatcher.userExists;

public class OracleDatabaseDriverIntegrationTest {

    private static final String TEST_USER1 = "TEST_USER1";
    private static final String TEST_USER2 = "TEST_USER2";
    private static final String TEST_PASSWORD = "password";

    private static DockerDriver dockerDriver;

    private DatabaseDriver testee;
    private Connection connection;
    private JdbcTemplate jdbcTemplate;

    @BeforeClass
    public static void setUpOnce() throws InterruptedException {
        Map<Integer, Integer> internalToExternalPortsMap = new HashMap<>();
        internalToExternalPortsMap.put(1521, 1521);

        dockerDriver = new DockerDriver(CONTAINER_NAME, ORACLE_XE_LATEST_IMAGE_NAME, internalToExternalPortsMap);
        dockerDriver.setUpDocker();

        Thread.sleep(10000);
    }

    @AfterClass
    public static void tearDownOnce() {
        dockerDriver.killAndRemoveContainer();
    }

    @Before
    public void setUp() throws SQLException {
        String username = "system";
        String password = "oracle";
        int port = 1521;
        String sid = "XE";

        OracleProperties properties = new OracleProperties(username, password, port, sid, TEST_USER1, TEST_USER2);

        testee = DatabaseDriverFactory.getInstance(properties);

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
        createUser(TEST_USER1, TEST_PASSWORD);
        createUser(TEST_USER2, TEST_PASSWORD);

        assertThat(testee.getDataSource(), userExists(TEST_USER1));
        assertThat(testee.getDataSource(), userExists(TEST_USER2));

        testee.tearDown();

        assertThat(testee.getDataSource(), not(userExists(TEST_USER1)));
        assertThat(testee.getDataSource(), not(userExists(TEST_USER2)));
    }

    @Test
    public void shouldTearDownAnExistingUsersWhichHasAnActiveSession() throws SQLException {
        createUser(TEST_USER1, TEST_PASSWORD);

        assertThat(testee.getDataSource(), userExists(TEST_USER1));

        OracleDataSource dataSource = new OracleDataSource();
        dataSource.setUser(TEST_USER1);
        dataSource.setPassword(TEST_PASSWORD);
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
}
