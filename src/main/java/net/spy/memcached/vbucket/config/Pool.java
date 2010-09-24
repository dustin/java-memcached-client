package net.spy.memcached.vbucket.config;

import java.net.URI;

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

    public URI getBucketsUri() {
        return bucketsUri;
    }

    void setBucketsUri(URI bucketsUri) {
        this.bucketsUri = bucketsUri;
    }
}
