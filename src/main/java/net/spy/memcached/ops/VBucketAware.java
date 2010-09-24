package net.spy.memcached.ops;

public interface VBucketAware {
    void setVBucket(int vbucket);
}
