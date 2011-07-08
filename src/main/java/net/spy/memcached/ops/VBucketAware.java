package net.spy.memcached.ops;

import net.spy.memcached.MemcachedNode;

import java.util.Collection;

/**
 * Operations that depend on a VBucket number being sent to the server
 * are required to implement this interface.
 */
public interface VBucketAware {
    void setVBucket(String key, short vbucket);
    short getVBucket(String key);
    Collection<MemcachedNode> getNotMyVbucketNodes();
    void addNotMyVbucketNode(MemcachedNode node);
    void setNotMyVbucketNodes(Collection<MemcachedNode> nodes);
}
