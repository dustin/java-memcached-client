package net.spy.memcached.vbucket.config;

import java.net.URI;
import java.util.List;

/**
 * Bucket configuration bean
 */
public class Bucket {
    // Bucket name
    private final String name;
    // vbuckets config
    private final Config vbuckets;
    // bucket's streaming uri
    private final URI streamingURI;

    // nodes list
    private List<Node> nodes;

    public Bucket(final String name, final Config vbuckets, final URI streamingURI, final List<Node> nodes) {
        this.name = name;
        this.vbuckets = vbuckets;
        this.streamingURI = streamingURI;
        this.nodes = nodes;
    }

    public String getName() {
        return name;
    }

    public Config getVbuckets() {
        return vbuckets;
    }

    public URI getStreamingURI() {
        return streamingURI;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bucket bucket = (Bucket) o;

        if (!name.equals(bucket.name)) return false;
        if (!nodes.equals(bucket.nodes)) return false;
        if (!vbuckets.equals(bucket.vbuckets)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + vbuckets.hashCode();
        result = 31 * result + nodes.hashCode();
        return result;
    }
}
