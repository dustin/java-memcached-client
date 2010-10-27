package net.spy.memcached.ops;

import net.spy.memcached.MemcachedNode;

import java.util.Collection;

public interface VBucketAware {
    void setVBucket(int vbucket);
    Collection<MemcachedNode> getNotMyVbucketNodes();
    void addNotMyVbucketNode(MemcachedNode node);
    void setNotMyVbucketNodes(Collection<MemcachedNode> nodes);
}
