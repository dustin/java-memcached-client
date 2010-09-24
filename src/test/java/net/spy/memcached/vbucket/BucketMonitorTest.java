package net.spy.memcached.vbucket;

import junit.framework.TestCase;
import net.spy.memcached.vbucket.config.ConfigurationParserMock;

import java.net.URI;

public class BucketMonitorTest extends TestCase {
    private static final String username = "";
    private static final String password = "";
    private static final String streamingUri = "http://127.0.0.1:8091/pools/default/bucketsStreaming/default";
    private static final String bucketname = "default";
    private static final ConfigurationParserMock configParser = new ConfigurationParserMock();
    public void testInstantiate() throws Exception {

        BucketMonitor bucketMonitor = new BucketMonitor(new URI(streamingUri), bucketname, username, password, configParser);
        assertEquals(username, bucketMonitor.getHttpUser());
        assertEquals(password, bucketMonitor.getHttpPass());
    }
    public void testObservable() throws Exception {
        BucketMonitor bucketMonitor = new BucketMonitor(new URI(streamingUri), bucketname, username, password, configParser);

        BucketObserverMock observer = new BucketObserverMock();
        bucketMonitor.addObserver(observer);

        bucketMonitor.addObserver(observer);

        bucketMonitor.startMonitor();

        assertTrue("Update for observer was not called.", observer.isUpdateCalled());
        bucketMonitor.shutdown();
    }
}
