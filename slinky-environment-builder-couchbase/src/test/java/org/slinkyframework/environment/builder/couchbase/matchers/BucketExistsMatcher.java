package org.slinkyframework.environment.builder.couchbase.matchers;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.cluster.ClusterManager;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.slinkyframework.environment.builder.couchbase.CouchbaseBuildDefinition;

import static org.slinkyframework.environment.builder.couchbase.local.ConnectionManager.getCluster;

public class BucketExistsMatcher extends TypeSafeMatcher<String> {

    private CouchbaseBuildDefinition buildDefinition;

    public BucketExistsMatcher(CouchbaseBuildDefinition buildDefinition) {
        this.buildDefinition = buildDefinition;
    }

    @Override
    protected boolean matchesSafely(String host) {
        Cluster cluster = getCluster(host);

        ClusterManager clusterManager = cluster.clusterManager(buildDefinition.getAdminUsername(), buildDefinition.getAdminPasssword());
        boolean hasBucket = clusterManager.hasBucket(buildDefinition.getBucketName());

        return hasBucket;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("bucket exists");
    }

    @Override
    protected void describeMismatchSafely(String host, Description mismatchDescription) {
        mismatchDescription.appendText("bucket does not exist");
    }
}
