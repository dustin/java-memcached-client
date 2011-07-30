package net.spy.memcached.spring;

import junit.framework.Assert;
import junit.framework.TestCase;

import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.DefaultHashAlgorithm;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.TestConfig;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;

import org.junit.Test;

/**
 * Test cases for the {@link MemcachedClientFactoryBean} implementation.
 *
 * @author Eran Harel
 */
public class MemcachedClientFactoryBeanTest extends TestCase {

  @Test
  public void testGetObject() throws Exception {
    final MemcachedClientFactoryBean factory = new MemcachedClientFactoryBean();
    factory.setDaemon(true);
    factory.setFailureMode(FailureMode.Cancel);
    factory.setHashAlg(DefaultHashAlgorithm.CRC32_HASH);
    factory.setProtocol(Protocol.BINARY);
    factory.setServers(TestConfig.IPV4_ADDR + ":22211 " + TestConfig.IPV4_ADDR + ":22212");
    factory.setShouldOptimize(true);
    final Transcoder<Object> transcoder = new SerializingTranscoder();
    factory.setTranscoder(transcoder);

    final MemcachedClient memcachedClient = (MemcachedClient)factory.getObject();

    Assert.assertEquals("servers", 2, memcachedClient.getUnavailableServers().size());
    Assert.assertSame("transcoder", transcoder, memcachedClient.getTranscoder());
  }

  @Test
  public void testGetObjectType() {
    Assert.assertEquals("object type", MemcachedClient.class, new MemcachedClientFactoryBean().getObjectType());
  }

}
