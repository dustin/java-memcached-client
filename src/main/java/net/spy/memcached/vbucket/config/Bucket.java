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

import java.net.URI;
import java.util.List;

/**
 * Bucket configuration bean.
 */
public class Bucket {
  // Bucket name
  private final String name;
  // configuration config
  private final Config configuration;
  // bucket's streaming uri
  private final URI streamingURI;

  // nodes list
  private final List<Node> nodes;

  public Bucket(String name, Config configuration, URI streamingURI,
      List<Node> nodes) {
    this.name = name;
    this.configuration = configuration;
    this.streamingURI = streamingURI;
    this.nodes = nodes;
  }

  public String getName() {
    return name;
  }

  public Config getConfig() {
    return configuration;
  }

  public URI getStreamingURI() {
    return streamingURI;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Bucket bucket = (Bucket) o;

    if (!name.equals(bucket.name)) {
      return false;
    }
    if (!nodes.equals(bucket.nodes)) {
      return false;
    }
    if (!configuration.equals(bucket.configuration)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + configuration.hashCode();
    result = 31 * result + nodes.hashCode();
    return result;
  }
}
