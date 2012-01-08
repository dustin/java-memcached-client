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

/**
 * Test the test protocol over IPv6.
 */
public class AsciiIPV6ClientTest extends AsciiClientTest {

  @Override
  protected void initClient(ConnectionFactory cf) throws Exception {
    client = new MemcachedClient(cf,
            AddrUtil.getAddresses(TestConfig.IPV6_ADDR
            + ":" + TestConfig.PORT_NUMBER));
  }

  @Override
  protected String getExpectedVersionSource() {
    if (TestConfig.defaultToIPV4()) {
      return String.valueOf(new InetSocketAddress(TestConfig.IPV4_ADDR,
              TestConfig.PORT_NUMBER));
    }
    return String.valueOf(new InetSocketAddress(TestConfig.IPV6_ADDR,
            TestConfig.PORT_NUMBER));
  }
}
