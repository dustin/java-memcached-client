package net.spy.memcached.util.twemproxy;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.compat.BaseMockCase;
import net.spy.memcached.util.twemproxy.SimpleTwemproxyNodeLocatorConfiguration;
import org.jmock.Mock;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;

public class SimpleTwemproxyNodeLocatorTest extends BaseMockCase
{
    SimpleTwemproxyNodeLocatorConfiguration nodeLocator;
    HashMap<SocketAddress, String> hostMap;

    public void setUp() {
        hostMap = new HashMap<SocketAddress, String>();
        hostMap.put(new InetSocketAddress("10.0.10.1", 11211), "10.0.10.1");
        hostMap.put(new InetSocketAddress("10.0.10.2", 11211), "10.0.10.2");
        hostMap.put(new InetSocketAddress("10.0.10.3", 11211), "10.0.10.3");
        hostMap.put(new InetSocketAddress("10.0.10.4", 11211), "10.0.10.4");

        nodeLocator = new SimpleTwemproxyNodeLocatorConfiguration(hostMap);

    }

    public void tearDown() {
        // empty
    }

    @Test
    public void testNodeRepetitions() {
        assertEquals(nodeLocator.getNodeRepetitions(), 160);
    }

    @Test
    public void testGetKeyForNode() {

        Mock m = mock(MemcachedNode.class);
        m.expects(once()).method("getSocketAddress").withNoArguments()
          .will(returnValue(new InetSocketAddress("10.0.10.1", 11211)));
        assertEquals(nodeLocator.getKeyForNode((MemcachedNode) m.proxy(), 4), "10.0.10.1-4");
    }
}
