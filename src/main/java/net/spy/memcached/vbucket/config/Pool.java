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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import net.spy.memcached.vbucket.ConfigurationException;

/**
 * Pool represents a collection of buckets.
 */
public class Pool {
  // pool name
  private final String name;
  // pool's uri
  private final URI uri;
  // pool's streaming uri
  private final URI streamingUri;
  // buckets related to this pool
  private URI bucketsUri;
  private final AtomicReference<Map<String, Bucket>> currentBuckets =
      new AtomicReference<Map<String, Bucket>>();

  public Pool(String name, URI uri, URI streamingUri) {
    this.name = name;
    this.uri = uri;
    this.streamingUri = streamingUri;
  }

  public String getName() {
    return name;
  }

  public URI getUri() {
    return uri;
  }

  public URI getStreamingUri() {
    return streamingUri;
  }

  /**
   * Get the current set of buckets known to this pool member.
   *
   * @return an atomic reference to the current Map of buckets
   */
  private AtomicReference<Map<String, Bucket>> getCurrentBuckets() {
    if (currentBuckets == null) {
      throw new ConfigurationException("Buckets were never populated.");
    }
    return currentBuckets;
  }

  public Map<String, Bucket> getROBuckets() {
    return Collections.unmodifiableMap(currentBuckets.get());
  }

  public URI getBucketsUri() {
    return bucketsUri;
  }

  void setBucketsUri(URI newBucketsUri) {
    this.bucketsUri = newBucketsUri;
  }

  public void replaceBuckets(Map<String, Bucket> replacingMap) {
    // TODO: replace this with a deep copy
    HashMap<String, Bucket> swapMap
      = new HashMap<String, Bucket>(replacingMap);
    currentBuckets.set(swapMap);
  }

  public boolean hasBucket(String bucketName) {
    boolean bucketFound = false;
    if (getCurrentBuckets().get().containsKey(bucketName)) {
      bucketFound = true;
    }
    return bucketFound;
  }
}
