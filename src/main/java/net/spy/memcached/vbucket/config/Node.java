package net.spy.memcached.vbucket.config;

import java.util.EnumMap;
import java.util.Map;

public class Node {
    private final Status status;
    private final String hostname;
    private final Map<Port, String> ports;

    public Node(Status status, String hostname, Map<Port, String> ports) {
        this.status = status;
        this.hostname = hostname;
        this.ports = new EnumMap<Port, String>(ports);
    }

    public Status getStatus() {
        return status;
    }

    public String getHostname() {
        return hostname;
    }

    public Map<Port, String> getPorts() {
        return ports;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node node = (Node) o;

        if (!hostname.equals(node.hostname)) return false;
        if (status != node.status) return false;
        if (!ports.equals(node.ports)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = status != null ? status.hashCode() : 0;
        result = 31 * result + hostname.hashCode();
        result = 31 * result + ports.hashCode();
        return result;
    }
}
