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

/**
 * A testConfig.
 */
public final class TestConfig {
  public static final String IPV4_PROP = "server.address_v4";
  public static final String IPV6_PROP = "server.address_v6";
  public static final String TEST_PROP = "test.type";
  public static final String PORT_PROP = "server.port_number";
  public static final String TYPE_TEST_UNIT = "unit";
  public static final String TYPE_TEST_CI = "ci";

  public static final String IPV4_ADDR = System.getProperty(IPV4_PROP,
      "127.0.0.1");
  public static final String IPV6_ADDR = resolveIpv6Addr();

  public static final int PORT_NUMBER =
      Integer.parseInt(System.getProperty(PORT_PROP, "11211"));

  public static final String TEST_TYPE = System.getProperty(TEST_PROP,
      TYPE_TEST_UNIT).toLowerCase();

  private TestConfig() {
    // Empty
  }

  private static String resolveIpv6Addr() {
    String ipv6 = System.getProperty(IPV6_PROP, "::1");
    // If the ipv4 address was set but the ipv6 address wasn't then
    // set the ipv6 address to use ipv4.
    if (!IPV4_ADDR.equals("127.0.0.1") && !IPV4_ADDR.equals("localhost")
        && ipv6.equals("::1")) {
      return "::ffff:" + IPV4_ADDR;
    }
    return ipv6;
  }

  public static boolean defaultToIPV4() {
    if (("::ffff:" + IPV4_ADDR).equals(IPV6_ADDR)) {
      return true;
    }
    return false;
  }

  public static boolean isCITest() {
    return TEST_TYPE.equals(TYPE_TEST_CI);
  }
}
