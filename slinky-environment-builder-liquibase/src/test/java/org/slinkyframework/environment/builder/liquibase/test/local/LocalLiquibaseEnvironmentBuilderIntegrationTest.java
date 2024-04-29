package org.slinkyframework.environment.builder.liquibase.test.local;

import org.junit.*;
import org.slinkyframework.environment.builder.liquibase.LiquibaseBuildDefinition;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseDriver;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseDriverFactory;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseProperties;
import org.slinkyframework.environment.builder.liquibase.local.LocalLiquibaseEnvironmentBuilder;
import org.slinkyframework.environment.docker.DockerDriver;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.slinkyframework.environment.builder.liquibase.test.matchers.OracleUserExistsMatcher.userExists;
import static org.slinkyframework.environment.builder.liquibase.test.utils.TestUtil.*;

public class LocalLiquibaseEnvironmentBuilderIntegrationTest {

    private static DockerDriver dockerDriver;
    private LocalLiquibaseEnvironmentBuilder testee = new LocalLiquibaseEnvironmentBuilder(TEST_HOST);

    private DatabaseProperties databaseProperties;
    private DatabaseDriver databaseDriver;

    @BeforeClass
    public static void setUpOnce()
    {
        dockerDriver = startDocker();
    }

    @Before
    public void setUp()
    {
        databaseDriver = DatabaseDriverFactory.getInstance(createLiquibaseBuildDefinition());
        databaseDriver.connect(TEST_HOST);
    }

    @AfterClass
    public static void tearDownOnce()
    {
        dockerDriver.killAndRemoveContainer();
    }

    @Test
    public void shouldTearDownANonExistentUser()
    {
        LiquibaseBuildDefinition liquibaseBuildDefinition = createLiquibaseBuildDefinitionWithUser("UNKNOWN");

        testee.tearDown(toSet(liquibaseBuildDefinition));
        // Success - no exception thrown
    }

    @Ignore
    @Test
    public void shouldSetUpAndthenTearDownAUser() throws SQLException
    {
        LiquibaseBuildDefinition liquibaseBuildDefinition = createLiquibaseBuildDefinition();

        testee.setUp(toSet(liquibaseBuildDefinition));

        commitTransaction(databaseDriver.getDataSource());

        assertThat(databaseDriver.getDataSource(), userExists(TEST_USER1));

        testee.tearDown(toSet(liquibaseBuildDefinition));
        assertThat(databaseDriver.getDataSource(), not(userExists(TEST_USER1)));
    }

    private Set<LiquibaseBuildDefinition> toSet(LiquibaseBuildDefinition liquibaseBuildDefinition)
    {
        Set<LiquibaseBuildDefinition> buildDefinitions = new HashSet<>();
        buildDefinitions.add(liquibaseBuildDefinition);
        return buildDefinitions;
    }

    private void commitTransaction(DataSource ds) throws SQLException
    {
        DataSourceUtils.getConnection(ds).commit();
    }
}
