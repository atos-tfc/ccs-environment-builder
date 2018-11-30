package org.slinkyframework.environment.builder.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.slinkyframework.environment.builder.EnvironmentBuilderContext;
import org.slinkyframework.environment.builder.EnvironmentManager;
import org.slinkyframework.environment.builder.EnvironmentManagerImpl;

public abstract class AbstractEnvironmentBuilderMojo extends AbstractMojo {

    @Parameter(property = "env.host", defaultValue = "localhost", readonly = true)
    private String host;

    @Parameter(property = "env.docker", defaultValue = "false", readonly = true)
    private boolean useDocker;

    @Parameter(property = "env.skip", defaultValue = "false", readonly = true)
    private boolean skip;

    @Parameter(property = "env.skipTearDown", defaultValue = "true", readonly = true)
    private boolean skipTearDown;

    private EnvironmentManager environmentManager;

    public AbstractEnvironmentBuilderMojo() {
    }

    // Used for testing
    public AbstractEnvironmentBuilderMojo(EnvironmentManager environmentManager) {
        this.environmentManager = environmentManager;
    }

    abstract void performBuild();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Environment build is skipped.");
        } else {
            if (environmentManager == null) {
                environmentManager = new EnvironmentManagerImpl();
            }

            performBuild();
        }
    }

    protected EnvironmentManager getEnvironmentManager() {
        return environmentManager;
    }

    public EnvironmentBuilderContext getEnvironmentBuilderContext() {
        return new EnvironmentBuilderContext(host, useDocker);
    }

    public void setUseDocker(boolean useDocker) {
        this.useDocker = useDocker;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public void setSkipTearDown(boolean skipTearDown) {
        this.skipTearDown = skipTearDown;
    }

    public boolean isSkipTearDown() {
        return skipTearDown;
    }
}
