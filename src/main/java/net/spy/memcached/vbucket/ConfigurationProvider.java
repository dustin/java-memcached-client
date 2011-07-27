package net.spy.memcached.vbucket;


import net.spy.memcached.vbucket.config.Bucket;

public interface ConfigurationProvider {

    /**
     * Connects to the REST service and retrieves the bucket configuration from the first pool available
     * @param bucketname bucketname
     * @return vbucket configuration
     * @throws ConfigurationException
     */
    Bucket getBucketConfiguration(String bucketname) throws ConfigurationException;

    /**
     * Subscribes for configuration updates
     * @param bucketName bucket name to receive configuration for
     * @param rec reconfigurable that will receive updates
     * @throws ConfigurationException
     */
    void subscribe(String bucketName, Reconfigurable rec) throws ConfigurationException;

    /**
     * Unsubscribe from updates on a given bucket and given reconfigurable
     * @param vbucketName bucket name
     * @param rec reconfigurable
     */
    void unsubscribe(String vbucketName, Reconfigurable rec);

    /**
     * Shutdowns a monitor connections to the REST service
     */
    void shutdown();

    /**
     * Retrieves a default bucket name i.e. 'default'
     * @return the anonymous bucket's name i.e. 'default'
     */
    String getAnonymousAuthBucket();
}
