package net.spy.memcached.vbucket;


import net.spy.memcached.vbucket.config.Bucket;

public interface ConfigurationProvider {
    Bucket getBucketConfiguration(String bucketname) throws ConfigurationException;

    void subscribe(String bucketName, Reconfigurable rec) throws ConfigurationException;

    void unsubscribe(String bucketName, Reconfigurable rec);

    void shutdown();

    String getAnonymousAuthBucket();
}
