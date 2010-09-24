package net.spy.memcached.vbucket;

import net.spy.memcached.vbucket.config.Bucket;

public interface Reconfigurable {
    void reconfigure(Bucket bucket);
}
