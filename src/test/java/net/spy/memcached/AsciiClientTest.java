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
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import net.spy.memcached.internal.OperationFuture;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.protocol.ascii.ExtensibleOperationImpl;
import org.junit.Test;

/**
 * This test assumes a server is running on the host specified in the
 * environment variable SPY_MC_TEST_SERVER or localhost:11211 by default.
 */
public class AsciiClientTest extends ProtocolBaseCase {

  public void testBadOperation() throws Exception {
    client.mconn.enqueueOperation("x",
        new ExtensibleOperationImpl(new OperationCallback() {
          public void complete() {
            System.err.println("Complete.");
          }

          public void receivedStatus(OperationStatus s) {
            System.err.println("Received a line.");
          }
        }) {

        @Override
        public void handleLine(String line) {
          System.out.println("Woo! A line!");
        }

        @Override
        public void initialize() {
          setBuffer(ByteBuffer.wrap("garbage\r\n".getBytes()));
        }
      });
  }

  @Override
  @Test(expected=UnsupportedOperationException.class)
  public void testSetReturnsCAS() {
  }
  @Override
  protected String getExpectedVersionSource() {
    return String.valueOf(new InetSocketAddress(TestConfig.IPV4_ADDR,
        TestConfig.PORT_NUMBER));
  }

  public void testAsyncCASResponse() throws InterruptedException,
    ExecutionException {
    String key = "testAsyncCASResponse";
    client.set(key, 300, key + "0");
    CASValue<Object> getsRes = client.gets(key);
    OperationFuture<CASResponse> casRes = client.asyncCAS(key, getsRes.getCas(),
      key + "1");
    CASResponse innerCasRes = casRes.get();
    try {
      casRes.getCas();
      fail("Expected an UnsupportedOperationException");
    } catch (UnsupportedOperationException ex) {
      //expected
    }
  }

}
