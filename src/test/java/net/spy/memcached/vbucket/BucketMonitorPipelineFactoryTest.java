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

package net.spy.memcached.vbucket;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * A BucketMonitorPipelineFactoryTest.
 */
public class BucketMonitorPipelineFactoryTest {

  @Before
  public void setUp() {

  }

  @Test
  public void testGetPipeline() throws Exception {
    BucketMonitorPipelineFactory factory = new BucketMonitorPipelineFactory();
    ChannelPipeline pipeline = factory.getPipeline();

    ChannelHandler decoder = pipeline.get("decoder");
    ChannelHandler encoder = pipeline.get("encoder");
    ChannelHandler handler = pipeline.get("handler");
    assertTrue(decoder instanceof HttpResponseDecoder);
    assertTrue(encoder instanceof HttpRequestEncoder);
    assertTrue(handler instanceof BucketUpdateResponseHandler);
    assertEquals(handler, pipeline.getLast());
    assertEquals(decoder, pipeline.getFirst());
  }
}
