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

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.StatusCode;

/**
 * This test assumes a binary server is running on the host specified int the
 * environment variable SPYMC_TEST_SERVER_V4 or localhost:11211 by default.
 */
public class BinaryClientTest extends ProtocolBaseCase {

  @Override
  protected void initClient() throws Exception {
    initClient(new BinaryConnectionFactory() {
      @Override
      public long getOperationTimeout() {
        return 15000;
      }

      @Override
      public FailureMode getFailureMode() {
        return FailureMode.RETRY;
      }
    });
  }

  @Override
  protected String getExpectedVersionSource() {
    return String.valueOf(new InetSocketAddress(TestConfig.IPV4_ADDR,
        TestConfig.PORT_NUMBER));
  }

  public void testDeleteWithCAS() throws Exception {
    final String key = "delete.with.cas";
    final long wrongCAS = 1234;

    OperationFuture<Boolean> setFuture = client.set(key, 0, "test");
    assertTrue(setFuture.get());

    assertFalse(client.delete(key, wrongCAS).get());
    assertTrue(client.delete(key, setFuture.getCas()).get());
    assertNull(client.get(key));
  }

  public void testCASAppendFail() throws Exception {
    final String key = "append.key";
    assertTrue(client.set(key, 5, "test").get());
    CASValue<Object> casv = client.gets(key);
    assertFalse(client.append(casv.getCas() + 1, key, "es").get());
    assertEquals("test", client.get(key));
  }

  public void testCASAppendSuccess() throws Exception {
    final String key = "append.key";
    assertTrue(client.set(key, 5, "test").get());
    CASValue<Object> casv = client.gets(key);
    assertTrue(client.append(casv.getCas(), key, "es").get());
    assertEquals("testes", client.get(key));
  }

  public void testCASPrependFail() throws Exception {
    final String key = "append.key";
    assertTrue(client.set(key, 5, "test").get());
    CASValue<Object> casv = client.gets(key);
    assertFalse(client.prepend(casv.getCas() + 1, key, "es").get());
    assertEquals("test", client.get(key));
  }

  public void testCASPrependSuccess() throws Exception {
    final String key = "append.key";
    assertTrue(client.set(key, 5, "test").get());
    CASValue<Object> casv = client.gets(key);
    assertTrue(client.prepend(casv.getCas(), key, "es").get());
    assertEquals("estest", client.get(key));
  }

  public void testAsyncCASResponse() {
    String key = "testAsyncCASResponse";
    client.set(key, 300, key + "0");
    CASValue<Object> getsRes = client.gets(key);
    OperationFuture<CASResponse> casRes = client.asyncCAS(key, getsRes.getCas(),
      key + "1");
    try {
      casRes.get();
      assertNotNull("OperationFuture is missing cas value.", casRes.getCas());
    } catch (InterruptedException ex) {
      fail("Interrupted while getting CASResponse");
    } catch (ExecutionException ex) {
      fail("Execution problem while getting CASResponse");
    }
    assertNotNull(casRes.getCas());
  }

  public void testAsyncCASWithExpiration() throws Exception {
    final String key = "casWithExpiration";
    final String value = "value";

    OperationFuture<Boolean> future = client.set(key, 0, value);
    assertTrue(future.get());

    OperationFuture<CASResponse> casFuture =
      client.asyncCAS(key, future.getCas(), 2, value);
    assertEquals(CASResponse.OK, casFuture.get());

    Thread.sleep(2500);
    assertNull(client.get(key));
  }

  @Override
  public void testKeyWithSpaces() throws Exception {
    String key = "key with spaces";
    client.set(key, 0, "");
    assertNotNull("Couldn't get the key with spaces in it.", client.get(key));
  }

  @Override
  public void testKeyWithNewline() throws Exception {
    String key = "Key\n";
    client.set(key, 0, "");
    assertNotNull(client.get(key));
  }

  @Override
  public void testKeyWithReturn() throws Exception {
    String key = "Key\r";
    client.set(key, 0, "");
    assertNotNull(client.get(key));
  }

  @Override
  public void testKeyWithASCIINull() throws Exception {
    String key = "Key\0";
    client.set(key, 0, "");
    assertNotNull(client.get(key));
  }

  @Override
  public void testGetBulkKeyWSpaces() throws Exception {
    String key = "Key key2";
    client.set(key, 0, "");
    Map<String, Object> bulkReturn = client.getBulk(key);
    assertTrue(bulkReturn.size() >= 1);
  }

  @Override
  protected void syncGetTimeoutsInitClient() throws Exception {
    initClient(new BinaryConnectionFactory() {
      @Override
      public long getOperationTimeout() {
        return 2;
      }

      @Override
      public int getTimeoutExceptionThreshold() {
        return 1000000;
      }
    });
  }

  public void testAddGetSetStatusCodes() throws Exception {
    OperationFuture<Boolean> set = client.set("statusCode1", 0, "value");
    set.get();
    assertEquals(StatusCode.SUCCESS, set.getStatus().getStatusCode());

    GetFuture<Object> get = client.asyncGet("statusCode1");
    get.get();
    assertEquals(StatusCode.SUCCESS, get.getStatus().getStatusCode());

    OperationFuture<Boolean> add = client.add("statusCode1", 0, "value2");
    add.get();
    assertEquals(StatusCode.ERR_EXISTS, add.getStatus().getStatusCode());
  }

  public void testAsyncIncrementWithDefault() throws Exception {
    String k = "async-incr-with-default";
    OperationFuture<Long> f = client.asyncIncr(k, 1, 5);
    assertEquals(StatusCode.SUCCESS, f.getStatus().getStatusCode());
    assertEquals(5, (long) f.get());

    f = client.asyncIncr(k, 1, 5);
    assertEquals(StatusCode.SUCCESS, f.getStatus().getStatusCode());
    assertEquals(6, (long) f.get());
  }

  public void testAsyncDecrementWithDefault() throws Exception {
    String k = "async-decr-with-default";
    OperationFuture<Long> f = client.asyncDecr(k, 4, 10);
    assertEquals(StatusCode.SUCCESS, f.getStatus().getStatusCode());
    assertEquals(10, (long) f.get());

    f = client.asyncDecr(k, 4, 10);
    assertEquals(StatusCode.SUCCESS, f.getStatus().getStatusCode());
    assertEquals(6, (long) f.get());
  }

}
