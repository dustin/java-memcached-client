package net.spy.memcached.vbucket;

import junit.framework.TestCase;
import net.spy.memcached.TestConfig;
import net.spy.memcached.vbucket.config.Bucket;

import java.util.List;
import java.net.URI;
import java.util.ArrayList;

public class ConfigurationProviderHTTPDownNodeTest extends TestCase {
    private static final String restUsr = "Administrator";
    private static final String restPwd = "password";
    private ConfigurationProviderHTTP configProvider;
    private static final String DEFAULT_BUCKET_NAME = "default";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
	List<URI> baseList = new ArrayList<URI>();
	baseList.add(new URI("http://bogus:8091/pools"));
	baseList.add(new URI("http://bogustoo:8091/pools"));
	baseList.add(new URI("http://" + TestConfig.IPV4_ADDR + ":8091/pools"));
	baseList.add(new URI("http://morebogus:8091/pools"));
        configProvider = new ConfigurationProviderHTTP(baseList, restUsr, restPwd);
        assertNotNull(configProvider);
    }

    public void testGetBucketConfiguration() throws Exception {
        Bucket bucket = configProvider.getBucketConfiguration(DEFAULT_BUCKET_NAME);
        assertNotNull(bucket);
    }
}
