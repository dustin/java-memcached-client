/**
 * Copyright (C) 2006-2009 Dustin Sallings
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

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Small test program that does a bunch of sets in a tight loop.
 */
public final class DoLotsOfSets {

  private DoLotsOfSets() {
    // Empty
  }

  public static void main(String[] args) throws Exception {
    // Create a client with a queue big enough to hold the 300,000 items
    // we're going to add.
    MemcachedClient client =
        new MemcachedClient(new DefaultConnectionFactory(350000, 32768),
        AddrUtil.getAddresses(TestConfig.IPV4_ADDR
            + ":" + TestConfig.PORT_NUMBER));
    long start = System.currentTimeMillis();
    byte[] toStore = new byte[26];
    Arrays.fill(toStore, (byte) 'a');
    for (int i = 0; i < 300000; i++) {
      client.set("k" + i, 300, toStore);
    }
    long added = System.currentTimeMillis();
    System.err.printf("Finished queuing in %sms%n", added - start);
    client.waitForQueues(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    long end = System.currentTimeMillis();
    System.err.printf("Completed everything in %sms (%sms to flush)%n", end
        - start, end - added);
    Map<String, Object> m = client.getBulk("k1", "k2", "k3", "k4", "k5",
        "k299999", "k299998", "k299997", "k299996");
    assert m.size() == 9 : "Expected 9 results, got " + m;
    client.shutdown();
  }
}
