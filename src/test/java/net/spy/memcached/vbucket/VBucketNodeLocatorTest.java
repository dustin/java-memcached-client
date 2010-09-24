package net.spy.memcached.vbucket;


import net.spy.memcached.MemcachedNode;
import net.spy.memcached.vbucket.config.Config;
import net.spy.memcached.vbucket.config.ConfigFactory;
import net.spy.memcached.vbucket.config.DefaultConfigFactory;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.net.InetSocketAddress;
import java.util.Arrays;

import junit.framework.TestCase;

public class VBucketNodeLocatorTest extends TestCase {

    private static final String configInEnvelope =
            "{ \"otherKeyThatIsIgnored\": 12345,\n" +
                    "\"vBucketServerMap\": \n" +
                    "{\n" +
                    "  \"hashAlgorithm\": \"CRC\",\n" +
                    "  \"numReplicas\": 2,\n" +
                    "  \"serverList\": [\"127.0.0.1:11211\", \"127.0.0.1:11210\", \"127.0.0.1:11212\"],\n" +
                    "  \"vBucketMap\":\n" +
                    "    [\n" +
                    "      [0, 1, 2],\n" +
                    "      [1, 2, 0],\n" +
                    "      [2, 1, -1],\n" +
                    "      [1, 2, 0]\n" +
                    "    ]\n" +
                    "}" +
                    "}";


    public void testGetPrimary() {
        MemcachedNode node1 = createMock(MemcachedNode.class);
        MemcachedNode node2 = createMock(MemcachedNode.class);
        MemcachedNode node3 = createMock(MemcachedNode.class);
        InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 11211);
        InetSocketAddress address2 = new InetSocketAddress("127.0.0.1", 11210);
        InetSocketAddress address3 = new InetSocketAddress("127.0.0.1", 11212);

        expect(node1.getSocketAddress()).andReturn(address1);
        expect(node2.getSocketAddress()).andReturn(address2);
        expect(node3.getSocketAddress()).andReturn(address3);

        replay(node1, node2, node3);

        ConfigFactory factory = new DefaultConfigFactory();
        Config config = factory.create(configInEnvelope);

        VBucketNodeLocator locator = new VBucketNodeLocator(Arrays.asList(node1, node2, node3), config);
        MemcachedNode resultNode = locator.getPrimary("key1");
        assertEquals(node1, resultNode);

        verify(node1, node2, node3);
    }
}
