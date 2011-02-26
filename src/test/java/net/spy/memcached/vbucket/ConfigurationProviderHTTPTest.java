package net.spy.memcached.vbucket;

import junit.framework.TestCase;
import net.spy.memcached.vbucket.config.Bucket;

import java.util.List;
import java.util.Arrays;
import java.net.URI;

public class ConfigurationProviderHTTPTest extends TestCase {
    private static final String restUsr = "Administrator";
    private static final String restPwd = "password";
    private ConfigurationProviderHTTP configProvider;
    private static final String DEFAULT_BUCKET_NAME = "default";
    private ReconfigurableMock reconfigurable = new ReconfigurableMock();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        List<URI> baseList = Arrays.asList(new URI("http://localhost:8091/pools"));
        configProvider = new ConfigurationProviderHTTP(baseList, restUsr, restPwd);
        assertNotNull(configProvider);
    }

    public void testGetBucketConfiguration() throws Exception {
        Bucket bucket = configProvider.getBucketConfiguration(DEFAULT_BUCKET_NAME);
        assertNotNull(bucket);
    }

    public void testSubscribe() throws Exception {
        configProvider.subscribe(DEFAULT_BUCKET_NAME, reconfigurable);
    }

    public void testUnsubscribe() throws Exception {
        configProvider.unsubscribe(DEFAULT_BUCKET_NAME,  reconfigurable);
    }

    public void testShutdown() throws Exception {
        configProvider.shutdown();
    }

    public void testGetAnonymousAuthBucket() throws Exception {
        assertEquals("default", configProvider.getAnonymousAuthBucket());
    }

    public void testBuildAuthHeader() {
	ConfigurationProviderHTTP.buildAuthHeader("foo", "bar");

    }
}
