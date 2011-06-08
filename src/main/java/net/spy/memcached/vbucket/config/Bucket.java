package net.spy.memcached.vbucket.config;

import java.net.URI;
import java.util.List;

/**
 * Bucket configuration bean
 */
public class Bucket {
    // Bucket name
    private final String name;
    // configuration config
    private final Config configuration;
    // bucket's streaming uri
    private final URI streamingURI;

    // nodes list
    private final List<Node> nodes;


    public Bucket(String name, Config configuration, URI streamingURI, List<Node> nodes) {
        this.name = name;
        this.configuration = configuration;
        this.streamingURI = streamingURI;
        this.nodes = nodes;
    }

    public String getName() {
        return name;
    }


    public Config getConfig() {
        return configuration;
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
        if (!configuration.equals(bucket.configuration)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + configuration.hashCode();
        result = 31 * result + nodes.hashCode();
        return result;
    }
}
