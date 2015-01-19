/**
 * Copyright (C) 2009-2011 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */

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
    factory.setHashAlg(DefaultHashAlgorithm.CRC_HASH);
    factory.setProtocol(Protocol.BINARY);
    factory.setServers(TestConfig.IPV4_ADDR + ":22211 " + TestConfig.IPV4_ADDR
        + ":22212");
    factory.setShouldOptimize(true);
    final Transcoder<Object> transcoder = new SerializingTranscoder();
    factory.setTranscoder(transcoder);

    factory.afterPropertiesSet();

    final MemcachedClient memcachedClient =
        (MemcachedClient) factory.getObject();

    Assert.assertEquals("servers", 2,
        memcachedClient.getUnavailableServers().size());
    Assert.assertSame("transcoder", transcoder,
        memcachedClient.getTranscoder());
  }

  @Test
  public void testGetObjectType() {
    Assert.assertEquals("object type", MemcachedClient.class,
        new MemcachedClientFactoryBean().getObjectType());
  }
}
