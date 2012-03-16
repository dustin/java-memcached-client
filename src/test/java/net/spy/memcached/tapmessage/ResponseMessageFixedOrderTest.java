/**
 * Copyright (C) 2009-2012 Couchbase, Inc.
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

package net.spy.memcached.tapmessage;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import org.junit.Before;

/**
 * Test ResponseMessage decoding when the server responds with the flags
 * correctly in network byteorder.
 *
 */
public class ResponseMessageFixedOrderTest extends ResponseMessageBaseCase {

  @Before
  @Override
  public void setUp() {


    expectedFlags = new LinkedList<TapResponseFlag>();
    expectedFlags.add(TapResponseFlag.TAP_FLAG_NETWORK_BYTE_ORDER);

    // see:
    // raw.github.com/
    // membase/memcached/branch-20/include/memcached/protocol_binary.h
    // bin request header + tap mutation + a key
    ByteBuffer binReqTapMutation = ByteBuffer.allocate(24+16+1+8);

   // bin request, tap mutation
    binReqTapMutation.put(0, (byte)0x80).put(1, (byte)0x41);
    binReqTapMutation.put(3, (byte)0x01); // key length
    binReqTapMutation.put(5, (byte)0x06); // datatype
    binReqTapMutation.put(11, (byte)0x09); // body length 1 key 8 value
    binReqTapMutation.put(27, (byte)0x04); // TAP_FLAG_NETWORK_BYTE_ORDER; fixed
    // the flags themselves, 0x0200 for four bytes starting at 32
    binReqTapMutation.put(34, (byte)0x02);

    // key and value
    binReqTapMutation.put(40, (byte)'a');
    binReqTapMutation.put(48, (byte)42);


    responsebytes = binReqTapMutation.array();

    instance = new ResponseMessage(responsebytes);
  }




}
