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

package net.spy.memcached.vbucket.config;

import java.util.EnumMap;
import java.util.Map;

/**
 * A Node.
 */
public class Node {
  private final Status status;
  private final String hostname;
  private final Map<Port, String> ports;

  public Node(Status status, String hostname, Map<Port, String> ports) {
    this.status = status;
    this.hostname = hostname;
    this.ports = new EnumMap<Port, String>(ports);
  }

  public Status getStatus() {
    return status;
  }

  public String getHostname() {
    return hostname;
  }

  public Map<Port, String> getPorts() {
    return ports;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Node node = (Node) o;

    if (!hostname.equals(node.hostname)) {
      return false;
    }
    if (status != node.status) {
      return false;
    }
    if (!ports.equals(node.ports)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = status != null ? status.hashCode() : 0;
    result = 31 * result + hostname.hashCode();
    result = 31 * result + ports.hashCode();
    return result;
  }
}
