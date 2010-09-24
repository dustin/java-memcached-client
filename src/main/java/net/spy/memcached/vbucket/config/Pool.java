package net.spy.memcached.vbucket.config;

import net.spy.memcached.vbucket.ConfigurationException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Collections;

/**
 * Pool represents a collection of buckets
 */
public class Pool {
    // pool name
    private final String name;
    // pool's uri
    private final URI uri;
    // pool's streaming uri
    private final URI streamingUri;
    // buckets related to this pool
    private URI bucketsUri;
    private final AtomicReference<Map<String, Bucket>> currentBuckets =
        new AtomicReference<Map<String, Bucket>>();

    public Pool(String name, URI uri, URI streamingUri) {
        this.name = name;
        this.uri = uri;
        this.streamingUri = streamingUri;
    }

    public String getName() {
        return name;
    }

    public URI getUri() {
        return uri;
    }

    public URI getStreamingUri() {
        return streamingUri;
    }

    /**
     * Get the current set of buckets known to this pool member.
     *
     * @return an atomic reference to the current Map of buckets
     */
    private AtomicReference<Map<String, Bucket>> getCurrentBuckets() {
	if (currentBuckets == null) {
	    throw new ConfigurationException("Buckets were never populated.");
	}
	return currentBuckets;
    }

    public Map<String, Bucket> getROBuckets() {
	return Collections.unmodifiableMap(currentBuckets.get());
    }

    public URI getBucketsUri() {
        return bucketsUri;
    }

    void setBucketsUri(URI bucketsUri) {
        this.bucketsUri = bucketsUri;
    }

    public void replaceBuckets(Map<String, Bucket> replacingMap) {
	HashMap<String, Bucket> swapMap = new HashMap<String, Bucket>(replacingMap); //TODO: replace this with a deep copy
	currentBuckets.set(swapMap);
    }

    public boolean hasBucket(String bucketName) {
	boolean bucketFound = false;
	if (getCurrentBuckets().get().containsKey(bucketName)) {
		bucketFound = true;
	}
	return bucketFound;
    }
}
