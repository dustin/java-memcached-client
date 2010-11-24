package net.spy.memcached.vbucket;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.NodeLocator;
import net.spy.memcached.vbucket.config.Config;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.net.InetSocketAddress;

/**
 * Implementation of the {@link NodeLocator} interface that contains vbucket hashing methods
 */
public class VBucketNodeLocator implements NodeLocator {

    private Map<String, MemcachedNode> nodesMap;

    private Config config;

    /**
     * Construct a VBucketNodeLocator over the given JSON configuration string.
     *
     * @param nodes
     * @param jsonConfig
     */
    public VBucketNodeLocator(List<MemcachedNode> nodes, Config jsonConfig) {
        super();
        setNodes(nodes);
        setConfig(jsonConfig);
    }

    /**
     * {@inheritDoc}
     */
    public MemcachedNode getPrimary(String k) {
        int vbucket = config.getVbucketByKey(k);
        int serverNumber = config.getMaster(vbucket);
        String server = config.getServer(serverNumber);
        // choose appropriate MemecachedNode according to config data
        return nodesMap.get(server);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<MemcachedNode> getSequence(String k) {
        return nodesMap.values().iterator();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<MemcachedNode> getAll() {
        return this.nodesMap.values();
    }

    /**
     * {@inheritDoc}
     */
    public NodeLocator getReadonlyCopy() {
        return this;
    }
    public void updateLocator(final List<MemcachedNode> nodes, final Config config) {
        setNodes(nodes);
        setConfig(config);
    }

    /**
     * Returns a vbucket index for the given key
     * @param key the key
     * @return vbucket index
     */
    public int getVBucketIndex(String key) {
        return config.getVbucketByKey(key);
    }

    private void setNodes(Collection<MemcachedNode> nodes) {
        Map<String, MemcachedNode> nodesMap = new HashMap<String, MemcachedNode>();
        for (MemcachedNode node : nodes) {
            InetSocketAddress addr = (InetSocketAddress) node.getSocketAddress();
            String address = addr.getAddress().getHostAddress() + ":" + addr.getPort();
            nodesMap.put(address, node);
        }

        this.nodesMap = nodesMap;
    }

    private void setConfig(final Config config) {
        this.config = config;
    }

    /**
     * Method returns the node that is not contained in the specified collection of the failed nodes
     * @param k the key
     * @param notMyVbucketNodes a collection of the nodes are excluded
     * @return The first MemcachedNode which mets requirements
     */
    public MemcachedNode getAlternative(String k, Collection<MemcachedNode> notMyVbucketNodes) {
        Collection<MemcachedNode> nodes = nodesMap.values();
        nodes.removeAll(notMyVbucketNodes);
        if (nodes.isEmpty()) {
            return null;
        } else {
            return nodes.iterator().next();
        }
    }
}
