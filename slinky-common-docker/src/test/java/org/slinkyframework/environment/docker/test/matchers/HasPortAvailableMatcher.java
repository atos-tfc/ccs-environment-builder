package org.slinkyframework.environment.docker.test.matchers;

import org.apache.commons.compress.utils.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slinkyframework.environment.builder.EnvironmentBuilderException;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.io.IOException;
import java.net.Socket;

import static java.lang.String.format;

public class HasPortAvailableMatcher extends TypeSafeMatcher<String> {

    private static final Logger LOG = LoggerFactory.getLogger(HasPortAvailableMatcher.class);
    private static final long THIRTY_SECONDS = 30000;
    private static final long ONE_SECOND = 1000;

    private int port;

    public HasPortAvailableMatcher(int port) {
        this.port = port;
    }

    @Override
    protected boolean matchesSafely(String host) {
        waitForPortToStart(host, port);
        return true; // Will eventually through an exception if does not exist
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Port '" + port + "' exists");
    }

    private void waitForPortToStart(String host, int port) {
        TimeoutRetryPolicy retryPolicy = new TimeoutRetryPolicy();
        retryPolicy.setTimeout(THIRTY_SECONDS);

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(ONE_SECOND);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setThrowLastExceptionOnExhausted(true);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        retryTemplate.execute(rc -> portInUse(host, port));
    }

    private boolean portInUse(String host, int port) {
        LOG.debug("Check whether {}:{} is in use", host, port);
        Socket s = null;
        try {
            s = new Socket(host, port);
            LOG.debug("{}:{} is in use", host, port);
            return true;
        } catch (IOException e) {
            LOG.debug("{}:{} is not in use", host, port);
            throw new EnvironmentBuilderException(format("Container not available at %s:%s", host, port), e);
        } finally {
            IOUtils.closeQuietly(s);
        }
    }

    public static HasPortAvailableMatcher hasPortAvailable(int port) {
        return new HasPortAvailableMatcher(port);
    }
}
