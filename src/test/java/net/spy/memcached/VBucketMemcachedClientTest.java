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

package net.spy.memcached;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


import junit.framework.TestCase;

import net.spy.memcached.vbucket.ConfigurationException;

/**
 * A VBucketMemcachedClientTest.
 */
public class VBucketMemcachedClientTest extends TestCase {
  public void testOps() throws Exception {
    MembaseClient mc = null;
    try {
      URI base = new URI("http://" + TestConfig.IPV4_ADDR + ":8091/pools");
      mc = new MembaseClient(Arrays.asList(base), "default", "Administrator",
          "password");
    } catch (IOException ex) {
      Logger.getLogger(VBucketMemcachedClientTest.class.getName()).log(
          Level.SEVERE, null, ex);
    } catch (ConfigurationException ex) {
      Logger.getLogger(VBucketMemcachedClientTest.class.getName()).log(
          Level.SEVERE, null, ex);
    } catch (URISyntaxException ex) {
      Logger.getLogger(VBucketMemcachedClientTest.class.getName()).log(
          Level.SEVERE, null, ex);
    }

    Integer i;
    for (i = 0; i < 10000; i++) {
      mc.set("test" + i, 0, i.toString());
    }
    mc.set("hello", 0, "world");
    String result = (String) mc.get("hello");
    assert (result.equals("world"));

    for (i = 0; i < 10000; i++) {
      String res = (String) mc.get("test" + i);
      assert (res.equals(i.toString()));
    }

    mc.shutdown(3, TimeUnit.SECONDS);
  }
}
