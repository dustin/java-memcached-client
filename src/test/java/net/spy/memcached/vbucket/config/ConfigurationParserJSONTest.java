package net.spy.memcached.vbucket.config;

import junit.framework.TestCase;

import java.util.Map;

public class ConfigurationParserJSONTest extends TestCase {

    private ConfigurationParser configParser = new ConfigurationParserJSON();
    private static final String DEFAULT_POOL_NAME = "default";


    public void testParseBase() throws Exception {
        Map<String, Pool> base = configParser.parseBase(baseString);
        assertNotNull(base);
        assertTrue(!base.isEmpty());
        Pool pool = base.get(DEFAULT_POOL_NAME);
        assertNotNull(pool);
        assertEquals(DEFAULT_POOL_NAME, pool.getName());
        assertNotNull(pool.getUri());
    }

    public void testParseBuckets() throws Exception {
        Map<String, Bucket> buckets = configParser.parseBuckets(bucketsString);
        for (Bucket bucket : buckets.values()) {
            checkBucket(bucket);
        }
    }

    public void testParseBucket() throws Exception {
        Bucket bucket = configParser.parseBucket(bucketString);
        checkBucket(bucket);
    }

    public void testLoadPool() throws Exception {
        Pool pool = new Pool(null, null, null);
        configParser.loadPool(pool, poolString);
        assertNotNull(pool.getBucketsUri());
    }
    private void checkBucket(Bucket bucket) throws Exception {
        assertNotNull("Bucket is null", bucket);
        assertNotNull(bucket.getName());
        assertNotNull(bucket.getStreamingURI());
        assertNotNull(bucket.getVbuckets());
    }
    private static final String baseString = "{\"pools\":[{\"name\":\"default\",\"uri\":\"/pools/default\"," +
            "\"streamingUri\":\"/poolsStreaming/default\"}],\"implementationVersion\":\"1.6.0beta3\"," +
            "\"componentsVersion\":{\"os_mon\":\"2.2.4\",\"mnesia\":\"4.4.12\",\"kernel\":\"2.13.4\"," +
            "\"sasl\":\"2.1.8\",\"ns_server\":\"1.6.0beta3\",\"menelaus\":\"1.6.0beta3\",\"stdlib\":\"1.16.4\"}}";

    private static final String bucketsString = "[{\"name\":\"Administrator\"," +
            "\"uri\":\"/pools/default/buckets/Administrator\"," +
            "\"streamingUri\":\"/pools/default/bucketsStreaming/Administrator\"," +
            "\"nodes\":[{\"uptime\":\"24548\",\"memoryTotal\":4290801664.0,\"memoryFree\":1292242944," +
            "\"mcdMemoryReserved\":3273,\"mcdMemoryAllocated\":3273,\"clusterMembership\":\"active\"," +
            "\"status\":\"healthy\",\"hostname\":\"192.168.1.35:8080\",\"version\":\"1.6.0beta3\"," +
            "\"os\":\"windows\",\"ports\":{\"proxy\":11211,\"direct\":11210}}]," +
            "\"stats\":{\"uri\":\"/pools/default/buckets/Administrator/stats\"}," +
            "\"vBucketServerMap\":{\"user\":\"Administrator\",\"password\":\"password\",\"hashAlgorithm\":\"CRC\"," +
            "\"numReplicas\":1,\"serverList\":[\"192.168.1.35:11210\"],\"vBucketMap\":[[0,-1]]}," +
            "\"replicaNumber\":1,\"quota\":{\"ram\":536870912,\"hdd\":307090161664.0},\"basicStats\":{\"opsPerSec\":0," +
            "\"diskFetches\":0,\"quotaPercentUsed\":2.380473166704178,\"diskUsed\":94598,\"memUsed\":12780068," +
            "\"itemCount\":0}},{\"name\":\"default\",\"bucketType\":\"membase\",\"authType\":\"sasl\"," +
            "\"saslPassword\":\"\",\"proxyPort\":0,\"uri\":\"/pools/default/buckets/default\"," +
            "\"streamingUri\":\"/pools/default/bucketsStreaming/default\"," +
            "\"flushCacheUri\":\"/pools/default/buckets/default/controller/doFlush\",\"nodes\":[{\"uptime\":\"24548\"," +
            "\"memoryTotal\":4290801664.0,\"memoryFree\":1292242944,\"mcdMemoryReserved\":3273," +
            "\"mcdMemoryAllocated\":3273,\"clusterMembership\":\"active\",\"status\":\"healthy\"," +
            "\"hostname\":\"192.168.1.35:8080\",\"version\":\"1.6.0beta3\",\"os\":\"windows\"," +
            "\"ports\":{\"proxy\":11211,\"direct\":11210}}],\"stats\":{\"uri\":\"/pools/default/buckets/default/stats\"}," +
            "\"vBucketServerMap\":{\"user\":\"default\",\"password\":\"\",\"hashAlgorithm\":\"CRC\",\"numReplicas\":1," +
            "\"serverList\":[\"192.168.1.35:11210\"],\"vBucketMap\":[[0,-1]]},\"replicaNumber\":1," +
            "\"quota\":{\"ram\":536870912,\"hdd\":2147483648.0},\"basicStats\":{\"opsPerSec\":0,\"diskFetches\":0," +
            "\"quotaPercentUsed\":2.380473166704178,\"diskUsed\":94598,\"memUsed\":12780068,\"itemCount\":0}}]";
    private static final String poolString = "{\"storageTotals\":{\"ram\":{\"quotaUsed\":1073741824," +
            "\"total\":4290801664.0,\"quotaTotal\":1073741824,\"used\":2993655808.0}," +
            "\"hdd\":{\"quotaUsed\":309237645312.0,\"usedByData\":189196,\"total\":309417967616.0," +
            "\"quotaTotal\":309417967616.0,\"used\":256816913121.0}},\"name\":\"default\"," +
            "\"nodes\":[{\"uptime\":\"24436\",\"memoryTotal\":4290801664.0,\"memoryFree\":1297145856," +
            "\"mcdMemoryReserved\":3273,\"mcdMemoryAllocated\":3273,\"clusterMembership\":\"active\",\"status\":\"healthy\"," +
            "\"hostname\":\"192.168.1.35:8080\",\"version\":\"1.6.0beta3\",\"os\":\"windows\"," +
            "\"ports\":{\"proxy\":11211,\"direct\":11210},\"otpNode\":\"ns_1@192.168.1.35\",\"otpCookie\":\"pbjgdbhhylfxkfol\"}]," +
            "\"buckets\":{\"uri\":\"/pools/default/buckets\"},\"controllers\":{\"addNode\":{\"uri\":\"/controller/addNode\"}," +
            "\"rebalance\":{\"uri\":\"/controller/rebalance\"},\"failOver\":{\"uri\":\"/controller/failOver\"}," +
            "\"reAddNode\":{\"uri\":\"/controller/reAddNode\"},\"ejectNode\":{\"uri\":\"/controller/ejectNode\"}," +
            "\"testWorkload\":{\"uri\":\"/pools/default/controller/testWorkload\"}},\"balanced\":true,\"rebalanceStatus\":\"none\"," +
            "\"rebalanceProgressUri\":\"/pools/default/rebalanceProgress\",\"stopRebalanceUri\":\"/controller/stopRebalance\"," +
            "\"stats\":{\"uri\":\"/pools/default/stats\"}}";

    private static final String bucketString = "{\"name\":\"Administrator\",\"uri\":\"/pools/default/buckets/Administrator\"," +
            "\"streamingUri\":\"/pools/default/bucketsStreaming/Administrator\",\"nodes\":[{\"uptime\":\"24548\"," +
            "\"memoryTotal\":4290801664.0,\"memoryFree\":1292242944,\"mcdMemoryReserved\":3273,\"mcdMemoryAllocated\":3273," +
            "\"clusterMembership\":\"active\",\"status\":\"healthy\",\"hostname\":\"192.168.1.35:8080\"," +
            "\"version\":\"1.6.0beta3\",\"os\":\"windows\",\"ports\":{\"proxy\":11211,\"direct\":11210}}]," +
            "\"stats\":{\"uri\":\"/pools/default/buckets/Administrator/stats\"},\"vBucketServerMap\":{\"user\":\"Administrator\"," +
            "\"password\":\"password\",\"hashAlgorithm\":\"CRC\",\"numReplicas\":1,\"serverList\":[\"192.168.1.35:11210\"]," +
            "\"vBucketMap\":[[0,-1]]},\"replicaNumber\":1,\"quota\":{\"ram\":536870912,\"hdd\":307090161664.0}," +
            "\"basicStats\":{\"opsPerSec\":0,\"diskFetches\":0,\"quotaPercentUsed\":2.380473166704178,\"diskUsed\":94598," +
            "\"memUsed\":12780068,\"itemCount\":0}}";
}
