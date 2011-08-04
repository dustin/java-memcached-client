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

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.tapmessage.ResponseMessage;

/**
 * A TapTest.
 */
public class TapTest extends ClientBaseCase {

  private static final long TAP_DUMP_TIMEOUT = 2000;

  @Override
  protected void initClient() throws Exception {
    initClient(new BinaryConnectionFactory() {
      @Override
      public long getOperationTimeout() {
        return 15000;
      }

      @Override
      public FailureMode getFailureMode() {
        return FailureMode.Retry;
      }
    });
  }

  public void testBackfill() throws Exception {
    if (TestConfig.isMembase()) {
      TapClient tc =
          new TapClient(AddrUtil.getAddresses(TestConfig.IPV4_ADDR + ":11210"));
      tc.tapBackfill(null, 5, TimeUnit.SECONDS);
      HashMap<String, Boolean> items = new HashMap<String, Boolean>();
      for (int i = 0; i < 25; i++) {
        client.set("key" + i, 0, "value" + i);
        items.put("key" + i + ",value" + i, new Boolean(false));
      }

      while (tc.hasMoreMessages()) {
        ResponseMessage m;
        if ((m = tc.getNextMessage()) != null) {
          String key = m.getKey() + "," + new String(m.getValue());
          if (items.containsKey(key)) {
            items.put(key, new Boolean(true));
          } else {
            fail();
          }
        }
      }
      checkTapKeys(items);
      assertTrue(client.flush().get().booleanValue());
    }
  }

  public void testTapDump() throws Exception {
    if (TestConfig.isMembase()) {
      TapClient tc =
          new TapClient(AddrUtil.getAddresses(TestConfig.IPV4_ADDR + ":11210"));

      HashMap<String, Boolean> items = new HashMap<String, Boolean>();
      for (int i = 0; i < 25; i++) {
        client.set("key" + i, 0, "value" + i).get();
        items.put("key" + i + ",value" + i, new Boolean(false));
      }
      tc.tapDump(null);

      long st = System.currentTimeMillis();
      while (tc.hasMoreMessages()) {
        if ((System.currentTimeMillis() - st) > TAP_DUMP_TIMEOUT) {
          fail("Tap dump took too long");
        }
        ResponseMessage m;
        if ((m = tc.getNextMessage()) != null) {
          String key = m.getKey() + "," + new String(m.getValue());
          if (items.containsKey(key)) {
            items.put(key, new Boolean(true));
          } else {
            fail();
          }
        }
      }
      checkTapKeys(items);
      assertTrue(client.flush().get().booleanValue());
    }
  }

  public void testTapBucketDoesNotExist() throws Exception {
    if (TestConfig.isMembase()) {
      TapClient client =
          new TapClient(Arrays.asList(new URI("http://" + TestConfig.IPV4_ADDR
              + ":8091/pools")), "abucket", "abucket", "apassword");

      try {
        client.tapBackfill(null, 5, TimeUnit.SECONDS);
      } catch (RuntimeException e) {
        System.err.println(e.getMessage());
        return;
      }
    }
  }

  private void checkTapKeys(HashMap<String, Boolean> items) {
    for (Entry<String, Boolean> kv : items.entrySet()) {
      if (!kv.getValue().booleanValue()) {
        fail();
      }
    }
  }
}
