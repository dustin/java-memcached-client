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

    public void testParseBucketsClustered() throws Exception {
	StringBuilder sb = new StringBuilder();
	sb.append(bucketsClusterString);
	sb.append(bucketsClusterString2);
        Map<String, Bucket> buckets = configParser.parseBuckets(sb.toString());
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

    private static final String bucketsString =
	"[\n" +
	"    {\n" +
	"        \"authType\": \"sasl\", \n" +
	"        \"basicStats\": {\n" +
	"            \"diskFetches\": 0, \n" +
	"            \"diskUsed\": 2912256, \n" +
	"            \"itemCount\": 0, \n" +
	"            \"memUsed\": 26001976, \n" +
	"            \"opsPerSec\": 0, \n" +
	"            \"quotaPercentUsed\": 24.797416687011719\n" +
	"        }, \n" +
	"        \"bucketType\": \"membase\", \n" +
	"        \"flushCacheUri\": \"/pools/default/buckets/default/controller/doFlush\", \n" +
	"        \"name\": \"default\", \n" +
	"        \"nodeLocator\": \"vbucket\", \n" +
	"        \"nodes\": [\n" +
	"            {\n" +
	"                \"clusterCompatibility\": 1, \n" +
	"                \"clusterMembership\": \"active\", \n" +
	"                \"hostname\": \"127.0.0.1:8091\", \n" +
	"                \"mcdMemoryAllocated\": 2871, \n" +
	"                \"mcdMemoryReserved\": 2871, \n" +
	"                \"memoryFree\": 193156000, \n" +
	"                \"memoryTotal\": 3764372000.0, \n" +
	"                \"os\": \"i386-apple-darwin10.6.0\", \n" +
	"                \"ports\": {\n" +
	"                    \"direct\": 11210, \n" +
	"                    \"proxy\": 11211\n" +
	"                }, \n" +
	"                \"replication\": 1.0, \n" +
	"                \"status\": \"healthy\", \n" +
	"                \"uptime\": \"57\", \n" +
	"                \"version\": \"1.6.5.3_5_gb78249f\"\n" +
	"            }\n" +
	"        ], \n" +
	"        \"proxyPort\": 0, \n" +
	"        \"quota\": {\n" +
	"            \"ram\": 104857600, \n" +
	"            \"rawRAM\": 104857600\n" +
	"        }, \n" +
	"        \"replicaNumber\": 1, \n" +
	"        \"saslPassword\": \"\", \n" +
	"        \"stats\": {\n" +
	"            \"uri\": \"/pools/default/buckets/default/stats\"\n" +
	"        }, \n" +
	"        \"streamingUri\": \"/pools/default/bucketsStreaming/default\", \n" +
	"        \"uri\": \"/pools/default/buckets/default\", \n" +
	"        \"vBucketServerMap\": {\n" +
	"            \"hashAlgorithm\": \"CRC\", \n" +
	"            \"numReplicas\": 1, \n" +
	"            \"serverList\": [\n" +
	"                \"127.0.0.1:11210\"\n" +
	"            ], \n" +
	"            \"vBucketMap\": [\n" +
	"                [\n" +
	"                    0, \n" +
	"                    -1\n" +
	"                ] \n" +
	"            ]\n" +
	"        }\n" +
	"    }\n" +
	"]\n";

    private static final String bucketsClusterString =
	"[\n" +
	"    {\n" +
	"        \"authType\": \"sasl\", \n" +
	"        \"basicStats\": {\n" +
	"            \"diskFetches\": 91, \n" +
	"            \"diskUsed\": 11399920640.0, \n" +
	"            \"itemCount\": 32112390, \n" +
	"            \"memUsed\": 11477801158.0, \n" +
	"            \"opsPerSec\": 4101, \n" +
	"            \"quotaPercentUsed\": 72.151368882753374\n" +
	"        }, \n" +
	"        \"bucketType\": \"membase\", \n" +
	"        \"flushCacheUri\": \"/pools/default/buckets/default/controller/doFlush\", \n" +
	"        \"name\": \"default\", \n" +
	"        \"nodeLocator\": \"vbucket\", \n" +
	"        \"nodes\": [\n" +
	"            {\n" +
	"                \"clusterCompatibility\": 1, \n" +
	"                \"clusterMembership\": \"active\", \n" +
	"                \"hostname\": \"172.16.10.32:8091\", \n" +
	"                \"mcdMemoryAllocated\": 6402, \n" +
	"                \"mcdMemoryReserved\": 6402, \n" +
	"                \"memoryFree\": 1497505792, \n" +
	"                \"memoryTotal\": 8391725056.0, \n" +
	"                \"os\": \"x86_64-unknown-linux-gnu\", \n" +
	"                \"ports\": {\n" +
	"                    \"direct\": 11210, \n" +
	"                    \"proxy\": 11211\n" +
	"                }, \n" +
	"                \"replication\": 1.0, \n" +
	"                \"status\": \"healthy\", \n" +
	"                \"uptime\": \"110393\", \n" +
	"                \"version\": \"1.6.5.4\"\n" +
	"            }, \n" +
	"            {\n" +
	"                \"clusterCompatibility\": 1, \n" +
	"                \"clusterMembership\": \"active\", \n" +
	"                \"hostname\": \"172.16.10.90:8091\", \n" +
	"                \"mcdMemoryAllocated\": 12865, \n" +
	"                \"mcdMemoryReserved\": 12865, \n" +
	"                \"memoryFree\": 10322313216.0, \n" +
	"                \"memoryTotal\": 16863064064.0, \n" +
	"                \"os\": \"x86_64-unknown-linux-gnu\", \n" +
	"                \"ports\": {\n" +
	"                    \"direct\": 11210, \n" +
	"                    \"proxy\": 11211\n" +
	"                }, \n" +
	"                \"replication\": 1.0, \n" +
	"                \"status\": \"healthy\", \n" +
	"                \"uptime\": \"110422\", \n" +
	"                \"version\": \"1.6.5.4\"\n" +
	"            }, \n" +
	"            {\n" +
	"                \"clusterCompatibility\": 1, \n" +
	"                \"clusterMembership\": \"active\", \n" +
	"                \"hostname\": \"172.16.10.97:8091\", \n" +
	"                \"mcdMemoryAllocated\": 12865, \n" +
	"                \"mcdMemoryReserved\": 12865, \n" +
	"                \"memoryFree\": 10408280064.0, \n" +
	"                \"memoryTotal\": 16863092736.0, \n" +
	"                \"os\": \"x86_64-unknown-linux-gnu\", \n" +
	"                \"ports\": {\n" +
	"                    \"direct\": 11210, \n" +
	"                    \"proxy\": 11211\n" +
	"                }, \n" +
	"                \"replication\": 1.0, \n" +
	"                \"status\": \"healthy\", \n" +
	"                \"uptime\": \"110411\", \n" +
	"                \"version\": \"1.6.5.4\"\n" +
	"            }\n" +
	"        ], \n" +
	"        \"proxyPort\": 0, \n" +
	"        \"quota\": {\n" +
	"            \"ram\": 15907946496.0, \n" +
	"            \"rawRAM\": 5302648832.0\n" +
	"        }, \n" +
	"        \"replicaNumber\": 1, \n" +
	"        \"saslPassword\": \"\", \n" +
	"        \"stats\": {\n" +
	"            \"uri\": \"/pools/default/buckets/default/stats\"\n" +
	"        }, \n" +
	"        \"streamingUri\": \"/pools/default/bucketsStreaming/default\", \n" +
	"        \"uri\": \"/pools/default/buckets/default\", \n" +
	"        \"vBucketServerMap\": {\n" +
	"            \"hashAlgorithm\": \"CRC\", \n" +
	"            \"numReplicas\": 1, \n" +
	"            \"serverList\": [\n" +
	"                \"172.16.10.32:11210\", \n" +
	"                \"172.16.10.90:11210\", \n" +
	"                \"172.16.10.97:11210\"\n" +
	"            ], \n" +
	"            \"vBucketMap\": [\n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n";

    private static final String bucketsClusterString2 = // overran the length of a static String!
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ], \n" +
	"                [\n" +
	"                    2, \n" +
	"                    0\n" +
	"                ], \n" +
	"                [\n" +
	"                    1, \n" +
	"                    2\n" +
	"                ], \n" +
	"                [\n" +
	"                    0, \n" +
	"                    1\n" +
	"                ]\n" +
	"            ]\n" +
	"        }\n" +
	"    }\n" +
	"]\n";

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
