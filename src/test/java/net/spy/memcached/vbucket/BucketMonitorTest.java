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

import java.net.URI;

import junit.framework.TestCase;

import net.spy.memcached.TestConfig;
import net.spy.memcached.vbucket.config.ConfigurationParserMock;

/**
 * A BucketMonitorTest.
 */
public class BucketMonitorTest extends TestCase {
  private static final String USERNAME = "";
  private static final String PASSWORD = "";
  private static final String STREAMING_URI = "http://" + TestConfig.IPV4_ADDR
      + ":8091/pools/default/bucketsStreaming/default";
  private static final String BUCKET_NAME = "default";
  private static final ConfigurationParserMock CONFIG_PARSER =
      new ConfigurationParserMock();

  public void testInstantiate() throws Exception {

    BucketMonitor bucketMonitor = new BucketMonitor(new URI(STREAMING_URI),
        BUCKET_NAME, USERNAME, PASSWORD, CONFIG_PARSER);
    assertEquals(USERNAME, bucketMonitor.getHttpUser());
    assertEquals(PASSWORD, bucketMonitor.getHttpPass());
  }

  public void testObservable() throws Exception {
    BucketMonitor bucketMonitor = new BucketMonitor(new URI(STREAMING_URI),
        BUCKET_NAME, USERNAME, PASSWORD, CONFIG_PARSER);

    BucketObserverMock observer = new BucketObserverMock();
    bucketMonitor.addObserver(observer);

    bucketMonitor.addObserver(observer);

    bucketMonitor.startMonitor();

    assertTrue("Update for observer was not called.",
        observer.isUpdateCalled());
    bucketMonitor.shutdown();
  }
}
