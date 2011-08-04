/**
 * Copyright (C) 2006-2009 Dustin Sallings
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

package net.spy.memcached.test;

import java.util.Random;
import java.util.concurrent.ExecutionException;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.ops.OperationErrorType;
import net.spy.memcached.ops.OperationException;

/**
 * Verify what happens when the memory is full on the server.
 *
 * This test expects a server running on localhost:
 *
 * memcached -U 11200 -p 11200 -m 32 -M
 */
public final class MemoryFullTest {

  private MemoryFullTest() {
    // Empty
  }

  public static void main(String[] args) throws Exception {
    // Verify assertions
    try {
      assert false;
      throw new RuntimeException("Assertions not enabled.");
    } catch (AssertionError e) {
      // OK
    }

    MemcachedClient c =
        new MemcachedClient(AddrUtil.getAddresses("localhost:11200"));
    boolean success = false;
    Random r = new Random();
    byte[] somebytes = new byte[71849];
    r.nextBytes(somebytes);
    try {
      for (int i = 0; i < 100000; i++) {
        c.set("k" + i, 3600, somebytes).get();
      }
    } catch (ExecutionException e) {
      assert e.getCause() instanceof OperationException;
      OperationException oe = (OperationException) e.getCause();
      assert oe.getType() == OperationErrorType.SERVER;
      assert oe.getMessage()
          .equals("SERVER_ERROR out of memory storing object");
      success = true;
    } finally {
      c.shutdown();
    }
    if (success) {
      System.out.println(":) Failed as expected.");
    } else {
      System.out.println(":( Unexpected failure.");
    }
  }
}
