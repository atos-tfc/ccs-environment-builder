package org.slinkyframework.environment.builder.liquibase.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slinkyframework.environment.builder.liquibase.LiquibaseBuildDefinition;
import org.slinkyframework.environment.builder.liquibase.drivers.DatabaseProperties;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class LiquibaseBuildDefinitionTest {

    private static final String TEST_CHANGE_LOG_XML = "test-changeLog.xml";
    private static final String TEST_NAME = "TEST";

    @Mock
    private DatabaseProperties mockDatabaseProperties;

    @Test
    public void shouldBeAbleToSpecifyDatabaseConnectionDetailsInBuildDefinition() {
        LiquibaseBuildDefinition testee = new LiquibaseBuildDefinition(TEST_NAME, mockDatabaseProperties, TEST_CHANGE_LOG_XML);

        assertThat(testee.getName(), is("TEST"));
        assertThat(testee.getDatabaseProperties(), is(mockDatabaseProperties));
        assertThat(testee.getChangeLogFile(), is(TEST_CHANGE_LOG_XML));
    }
}
