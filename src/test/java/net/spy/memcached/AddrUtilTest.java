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

package net.spy.memcached;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

/**
 * Test the AddrUtils stuff.
 */
public class AddrUtilTest extends TestCase {

  public void testSingle() throws Exception {
    List<HostPort> addrs = AddrUtil.getAddresses("www.google.com:80");
    assertEquals(1, addrs.size());
    assertEquals("www.google.com", addrs.get(0).getHostName());
    assertEquals(80, addrs.get(0).getPort());
  }

  public void testTwo() throws Exception {
    List<HostPort> addrs =
        AddrUtil.getAddresses("www.google.com:80 www.yahoo.com:81");
    assertEquals(2, addrs.size());
    assertEquals("www.google.com", addrs.get(0).getHostName());
    assertEquals(80, addrs.get(0).getPort());
    assertEquals("www.yahoo.com", addrs.get(1).getHostName());
    assertEquals(81, addrs.get(1).getPort());
  }

  public void testThree() throws Exception {
    List<HostPort> addrs = AddrUtil
        .getAddresses(" ,  www.google.com:80 ,, ,, www.yahoo.com:81 , ,,");
    assertEquals(2, addrs.size());
    assertEquals("www.google.com", addrs.get(0).getHostName());
    assertEquals(80, addrs.get(0).getPort());
    assertEquals("www.yahoo.com", addrs.get(1).getHostName());
    assertEquals(81, addrs.get(1).getPort());
  }

  public void testBrokenHost() throws Exception {
    String s = "www.google.com:80 www.yahoo.com:81:more";
    try {
      List<HostPort> addrs = AddrUtil.getAddresses(s);
      fail("Expected failure, got " + addrs);
    } catch (NumberFormatException e) {
      e.printStackTrace();
      assertEquals("For input string: \"more\"", e.getMessage());
    }
  }

  public void testBrokenHost2() throws Exception {
    String s = "www.google.com:80 www.yahoo.com";
    try {
      List<HostPort> addrs = AddrUtil.getAddresses(s);
      fail("Expected failure, got " + addrs);
    } catch (IllegalArgumentException e) {
      assertEquals("Invalid server ``www.yahoo.com'' in list:  " + s,
          e.getMessage());
    }
  }

  public void testBrokenList() throws Exception {
    String s = "";
    try {
      List<HostPort> addrs = AddrUtil.getAddresses(s);
      fail("Expected failure, got " + addrs);
    } catch (IllegalArgumentException e) {
      assertEquals("No hosts in list:  ``''", e.getMessage());
    }
  }

  public void testBrokenList2() throws Exception {
    String s = "   ";
    try {
      List<HostPort> addrs = AddrUtil.getAddresses(s);
      fail("Expected failure, got " + addrs);
    } catch (IllegalArgumentException e) {
      assertEquals("No hosts in list:  ``   ''", e.getMessage());
    }
  }

  public void testNullList() throws Exception {
    String s = null;
    try {
      List<HostPort> addrs = AddrUtil.getAddresses(s);
      fail("Expected failure, got " + addrs);
    } catch (NullPointerException e) {
      assertEquals("Null host list", e.getMessage());
    }
  }

  public void testIPv6Host() throws Exception {
    List<HostPort> addrs = AddrUtil.getAddresses("::1:80");
    assertEquals(1, addrs.size());

    Set<String> validLocalhostNames = new HashSet<String>();
    validLocalhostNames.add("localhost");
    validLocalhostNames.add("ip6-localhost");
    validLocalhostNames.add("0:0:0:0:0:0:0:1");
    validLocalhostNames.add("localhost6.localdomain6");
    assert (validLocalhostNames.contains(addrs.get(0).getAddress().getHostName()));
    assertEquals(80, addrs.get(0).getPort());
  }
}
