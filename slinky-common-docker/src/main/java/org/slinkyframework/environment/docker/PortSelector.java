package org.slinkyframework.environment.docker;

import org.slinkyframework.environment.builder.EnvironmentBuilderException;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Optional;

import static java.lang.String.format;

public class PortSelector {

    private static final String ENVIRONMENT_DOCKER_PORT = "ENV_DOCKER_PORT_%s";

    public static int selectFreePort() {
        try {
            ServerSocket s = new ServerSocket(0);
            return s.getLocalPort();
        } catch (IOException e) {
            throw new EnvironmentBuilderException("Unable to find a free port", e);
        }
    }

    public static int selectPort(int internalPort) {
        int externalPort = internalPort;

        Optional<String> specifiedPort = getPortFromEnvironment(internalPort);

        if (specifiedPort.isPresent()) {
            if (specifiedPort.get().trim().equals("")) {
                externalPort = selectFreePort();
            } else {
                externalPort = Integer.parseInt(specifiedPort.get());
            }
        }

        return externalPort;
    }

    private static Optional<String> getPortFromEnvironment(int internalPort) {
        return Optional.ofNullable(System.getenv(determineEnvironmentVariable(internalPort)));
    }

    private static String determineEnvironmentVariable(int internalPort) {
        return format(ENVIRONMENT_DOCKER_PORT, internalPort);
    }
}
