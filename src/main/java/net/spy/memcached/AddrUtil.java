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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Convenience utilities for simplifying common address parsing.
 */
public final class AddrUtil {

  private AddrUtil() {
    // Empty
  }

  /**
   * Split a string containing whitespace or comma separated host or IP
   * addresses and port numbers of the form "host:port host2:port" or
   * "host:port, host2:port" into a List of InetSocketAddress instances suitable
   * for instantiating a MemcachedClient.
   *
   * Note that colon-delimited IPv6 is also supported. For example: ::1:11211
   */
  public static List<InetSocketAddress> getAddresses(String s) {
    if (s == null) {
      throw new NullPointerException("Null host list");
    }
    if (s.trim().equals("")) {
      throw new IllegalArgumentException("No hosts in list:  ``" + s + "''");
    }
    ArrayList<InetSocketAddress> addrs = new ArrayList<InetSocketAddress>();

    for (String hoststuff : s.split("(?:\\s|,)+")) {
      if (hoststuff.equals("")) {
        continue;
      }

      int finalColon = hoststuff.lastIndexOf(':');
      if (finalColon < 1) {
        throw new IllegalArgumentException("Invalid server ``" + hoststuff
            + "'' in list:  " + s);
      }
      String hostPart = hoststuff.substring(0, finalColon);
      String portNum = hoststuff.substring(finalColon + 1);

      addrs.add(new InetSocketAddress(hostPart, Integer.parseInt(portNum)));
    }
    assert !addrs.isEmpty() : "No addrs found";
    return addrs;
  }

  public static List<InetSocketAddress> getAddresses(List<String> servers) {
    ArrayList<InetSocketAddress> addrs =
        new ArrayList<InetSocketAddress>(servers.size());
    for (String server : servers) {
      int finalColon = server.lastIndexOf(':');
      if (finalColon < 1) {
        throw new IllegalArgumentException("Invalid server ``" + server
            + "'' in list:  " + server);
      }
      String hostPart = server.substring(0, finalColon);
      String portNum = server.substring(finalColon + 1);

      addrs.add(new InetSocketAddress(hostPart, Integer.parseInt(portNum)));
    }
    if (addrs.isEmpty()) {
      // servers was passed in empty, and shouldn't have been
      throw new IllegalArgumentException("servers cannot be empty");
    }
    return addrs;
  }

  public static List<InetSocketAddress>
  getAddressesFromURL(List<URL> servers) {
    ArrayList<InetSocketAddress> addrs =
      new ArrayList<InetSocketAddress>(servers.size());
    for (URL server : servers) {
      addrs.add(new InetSocketAddress(server.getHost(), server.getPort()));
    }
    return addrs;
  }
}
