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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map.Entry;

import junit.framework.TestCase;

import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.protocol.binary.BinaryOperationFactory;

/**
 * An ErrorCodeTest.
 */
public class ErrorCodeTest extends TestCase {

  public void testErrorCodes() throws Exception {
    HashMap<Byte, String> errMap = new HashMap<Byte, String>();
    OperationFactory opFact = new BinaryOperationFactory();

    errMap.put(Byte.valueOf((byte) 0x01), "NOT FOUND");
    errMap.put(Byte.valueOf((byte) 0x02), "EXISTS");
    errMap.put(Byte.valueOf((byte) 0x03), "2BIG");
    errMap.put(Byte.valueOf((byte) 0x04), "INVAL");
    errMap.put(Byte.valueOf((byte) 0x05), "NOT STORED");
    errMap.put(Byte.valueOf((byte) 0x06), "DELTA BAD VAL");
    errMap.put(Byte.valueOf((byte) 0x07), "NOT MY VBUCKET");
    errMap.put(Byte.valueOf((byte) 0x81), "UNKNOWN COMMAND");
    errMap.put(Byte.valueOf((byte) 0x82), "NO MEM");
    errMap.put(Byte.valueOf((byte) 0x83), "NOT SUPPORTED");
    errMap.put(Byte.valueOf((byte) 0x84), "INTERNAL ERROR");
    errMap.put(Byte.valueOf((byte) 0x85), "BUSY");
    errMap.put(Byte.valueOf((byte) 0x86), "TEMP FAIL");

    int opaque = 0;
    for (final Entry<Byte, String> err : errMap.entrySet()) {
      byte[] b = new byte[24 + err.getValue().length()];
      b[0] = (byte) 0x81;
      b[7] = err.getKey();
      b[11] = (byte) err.getValue().length();
      b[15] = (byte) ++opaque;
      System.arraycopy(err.getValue().getBytes(), 0, b, 24,
          err.getValue().length());

      GetOperation op = opFact.get("key", new GetOperation.Callback() {
        public void receivedStatus(OperationStatus s) {
          assert !s.isSuccess();
          assert err.getValue().equals(s.getMessage());
        }

        public void gotData(String k, int flags, byte[] data) {

        }

        public void complete() {
        }
      });
      ByteBuffer bb = ByteBuffer.wrap(b);
      bb.flip();
      op.readFromBuffer(bb);
    }
  }
}
