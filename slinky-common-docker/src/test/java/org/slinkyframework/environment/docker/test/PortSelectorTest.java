package org.slinkyframework.environment.docker.test;

import org.junit.Test;
import org.slinkyframework.environment.docker.PortSelector;

import java.lang.reflect.Field;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class PortSelectorTest {

    @Test
    public void shouldSelectAFreePort() {

        int selectedPort = PortSelector.selectFreePort();

        System.out.println(selectedPort);

        assertThat("Port", selectedPort, is(not(nullValue())));
    }

    @Test
    public void shouldSelectExternalPortAsSameAsInternalPortByDefault() {
        int internalPort = 8080;

        int selectedPort = PortSelector.selectPort(internalPort);

        System.out.println(selectedPort);

        assertThat("Port", selectedPort, is(internalPort));
    }

    @Test
    public void shouldSelectExternalPortFromEnvironmentVariable() throws Exception {
        int internalPort = 8081;
        int externalPort = 9091;

        injectEnvironmentVariable("ENV_DOCKER_PORT_" + internalPort, String.valueOf(externalPort));

        int selectedPort = PortSelector.selectPort(internalPort);

        System.out.println(selectedPort);

        assertThat("Port", selectedPort, is(externalPort));
    }

    @Test
    public void shouldSelectFreePortDueToEmptyEnvironmentVariable() throws Exception {
        int internalPort = 8082;

        injectEnvironmentVariable("ENV_DOCKER_PORT_" + internalPort, "");

        int selectedPort = PortSelector.selectPort(internalPort);

        System.out.println(selectedPort);

        assertThat("Port", selectedPort, is(not(nullValue())));
        assertThat("Port", selectedPort, is(not(internalPort)));
    }

    private static void injectEnvironmentVariable(String key, String value) throws Exception {
        Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");

        Field unmodifiableMapField = getAccessibleField(processEnvironment, "theUnmodifiableEnvironment");
        Object unmodifiableMap = unmodifiableMapField.get(null);
        injectIntoUnmodifiableMap(key, value, unmodifiableMap);

        Field mapField = getAccessibleField(processEnvironment, "theEnvironment");
        Map<String, String> map = (Map<String, String>) mapField.get(null);
        map.put(key, value);
    }

    private static Field getAccessibleField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    private static void injectIntoUnmodifiableMap(String key, String value, Object map) throws ReflectiveOperationException {
        Class unmodifiableMap = Class.forName("java.util.Collections$UnmodifiableMap");
        Field field = getAccessibleField(unmodifiableMap, "m");
        Object obj = field.get(map);
        ((Map<String, String>) obj).put(key, value);
    }
}
