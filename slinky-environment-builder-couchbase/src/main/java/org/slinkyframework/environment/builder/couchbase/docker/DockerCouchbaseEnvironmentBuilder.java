package org.slinkyframework.environment.builder.couchbase.docker;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slinkyframework.environment.builder.EnvironmentBuilder;
import org.slinkyframework.environment.builder.EnvironmentBuilderException;
import org.slinkyframework.environment.builder.couchbase.CouchbaseBuildDefinition;
import org.slinkyframework.environment.builder.couchbase.local.LocalCouchbaseEnvironmentBuilder;
import org.slinkyframework.environment.docker.DockerDriver;
import org.slinkyframework.environment.docker.PortSelector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Creates and starts a Couchbase Docker container and the defined buckets within.
 * It then kills and removes the container when environment torn down.
 *
 * NOTE: Assumes the standard Docker Machine environment variables are set:
 *         DOCKER_TLS_VERIFY, DOCKER_HOST, DOCKER_CERT_PATH and DOCKER_MACHINE_NAME
 * Make sure these are set before running IDE or Maven build.
 */
public class DockerCouchbaseEnvironmentBuilder implements EnvironmentBuilder<CouchbaseBuildDefinition> {

    private static final Logger LOG = LoggerFactory.getLogger(DockerCouchbaseEnvironmentBuilder.class);

    public static final String CONTAINER_NAME = "slinky_couchbase";
    public static final String COUCHBASE_LATEST_IMAGE_NAME = "couchbase:latest";
    public  static final int[] COUCHBASE_PORTS = { 8091, 8092, 8093, 8094, 11207, 11210, 11211 };

    private final LocalCouchbaseEnvironmentBuilder localEnvironmentBuilder;
    private final Map<Integer, Integer> internalToExternalPortsMap = new HashMap<>();
    private final DockerDriver dockerDriver;

    public DockerCouchbaseEnvironmentBuilder(LocalCouchbaseEnvironmentBuilder localEnvironmentBuilder) {
        this.localEnvironmentBuilder = localEnvironmentBuilder;

        for (int port: COUCHBASE_PORTS) {
            internalToExternalPortsMap.put(port, PortSelector.selectPort(port));
        }

        dockerDriver = new DockerDriver(CONTAINER_NAME, COUCHBASE_LATEST_IMAGE_NAME, internalToExternalPortsMap);
    }

    public Map<Integer, Integer> getInternalToExternalPortsMap() {
        return internalToExternalPortsMap;
    }

    @Override
    public void setUp(Set<CouchbaseBuildDefinition> buildDefinitions) {
        dockerDriver.setUpDocker();

        dockerDriver.waitFor(this::createCouchbaseCluster);
        dockerDriver.waitFor(this::initialiseCouchbaseCluster);

        localEnvironmentBuilder.setUp(buildDefinitions);
    }

    @Override
    public void tearDown(Set<CouchbaseBuildDefinition> buildDefinitions) {
        LOG.info("Tearing down Couchbase Docker container '{}'", CONTAINER_NAME);

        localEnvironmentBuilder.tearDown(buildDefinitions);

        dockerDriver.killAndRemoveContainer();
    }

    @Override
    public void cleanUp() {
        localEnvironmentBuilder.cleanUp();
    }

    private void createCouchbaseCluster(DockerClient docker, String containerId) {
        LOG.info("Creating Couchbase cluster");

        try {
            ExecCreation execCreation = docker.execCreate(containerId,
                    new String[]{
                            "/opt/couchbase/bin/couchbase-cli"
                            , "setting-cluster"
                            , "-c", "127.0.0.1:8091"
                            , "-u", "admin"
                            , "-p", "password"
                            , "--cluster-name", "couchbase_cluster"
                            , "--cluster-ramsize=500"
                    }
                    , DockerClient.ExecCreateParam.attachStdout(true)
                    , DockerClient.ExecCreateParam.attachStderr(true)
            );

            String log;
            try (final LogStream stream = docker.execStart(execCreation.id())) {
                log = stream.readFully();
            }

            final ExecState state = docker.execInspect(execCreation.id());

            if (state.exitCode() == 0) {
                LOG.debug("Couchbase cluster created");
            } else {
                LOG.warn("Unable to create Couchbase cluster:\n{}", log);
                throw new EnvironmentBuilderException("Unable to create Couchbase cluster");
            }
        } catch (DockerException | InterruptedException e) {
            LOG.warn("Unable to create Couchbase cluster: {}", e.getMessage());
            throw new EnvironmentBuilderException("Unable to create Couchbase cluster", e);
        }
    }

    private void initialiseCouchbaseCluster(DockerClient docker, String containerId) {
        LOG.info("Initialising Couchbase cluster");

        try {
            final ExecCreation execCreation = docker.execCreate(containerId,
                    new String[]{
                            "/opt/couchbase/bin/couchbase-cli"
                            , "cluster-init"
                            , "-c", "127.0.0.1:8091"
                            , "-u", "admin"
                            , "-p", "password"
                            , "--services=data,index,query,fts"
                            , "--index-storage-setting=default"
                            , "--cluster-ramsize=500"
                            , "--cluster-index-ramsize=500"
                    }
                    , DockerClient.ExecCreateParam.attachStdout(true)
                    , DockerClient.ExecCreateParam.attachStderr(true)
            );

            String log;
            try (final LogStream stream = docker.execStart(execCreation.id())) {
                log = stream.readFully();
            }

            final ExecState state = docker.execInspect(execCreation.id());

            if (state.exitCode() == 0) {
                LOG.debug("Couchbase cluster initialised");
            } else {
                LOG.warn("Unable to initialise Couchbase cluster:\n{}", log);
                throw new EnvironmentBuilderException("Unable to initialise Couchbase cluster");
            }

        } catch (DockerException | InterruptedException e) {
            LOG.warn("Unable to initialise Couchbase cluster: {}", e.getMessage());
            throw new EnvironmentBuilderException("Unable to initialise Couchbase cluster", e);
        }
    }
}
