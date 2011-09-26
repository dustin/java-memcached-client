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
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.spy.memcached.DefaultHashAlgorithm;

/**
 * A ConfigurationParserMock.
 */
public class ConfigurationParserMock implements ConfigurationParser {
  private boolean parseBaseCalled = false;
  private boolean parseBucketsCalled = false;
  private boolean parseBucketCalled = false;
  private boolean loadPoolCalled = false;
  private String poolName = "default";
  private String poolUri = "/pools/default";
  private String poolStreamingUri = "/poolsStreaming/default";
  private String bucketName = "Administrator";
  private DefaultConfig vbuckets =
    new DefaultConfig(DefaultHashAlgorithm.NATIVE_HASH, 1, 1, 1, null, null, null);
  private String bucketsUri = "/pools/default/buckets";
  private String bucketStreamingUri =
      "/pools/default/bucketsStreaming/Administrator";
  private List<Node> nodes = Collections.singletonList(new Node(Status.healthy,
      "localhost", Collections.singletonMap(Port.direct, "11210")));

  public Map<String, Pool> parseBase(String base) throws ParseException {
    Map<String, Pool> result = new HashMap<String, Pool>();
    try {
      parseBaseCalled = true;
      Pool pool =
          new Pool(poolName, new URI(poolUri), new URI(poolStreamingUri));
      result.put(poolName, pool);
    } catch (URISyntaxException e) {
      throw new ParseException(e.getMessage(), 0);
    }
    return result;
  }

  public Map<String, Bucket> parseBuckets(String buckets)
    throws ParseException {
    Map<String, Bucket> result = new HashMap<String, Bucket>();
    try {
      parseBucketsCalled = true;
      Bucket bucket =
          new Bucket(bucketName, vbuckets, new URI(bucketStreamingUri), nodes);
      result.put(bucketName, bucket);
    } catch (URISyntaxException e) {
      throw new ParseException(e.getMessage(), 0);
    }
    return result;
  }

  public Bucket parseBucket(String sBucket) throws ParseException {
    parseBucketCalled = true;
    try {
      parseBucketsCalled = true;
      return new Bucket(bucketName, vbuckets, new URI(bucketStreamingUri),
          nodes);
    } catch (URISyntaxException e) {
      throw new ParseException(e.getMessage(), 0);
    }

  }

  public void loadPool(Pool pool, String sPool) throws ParseException {
    try {
      loadPoolCalled = true;
      pool.setBucketsUri(new URI(bucketsUri));
    } catch (URISyntaxException e) {
      throw new ParseException(e.getMessage(), 0);
    }
  }

  public boolean isParseBaseCalled() {
    return parseBaseCalled;
  }

  public boolean isParseBucketsCalled() {
    return parseBucketsCalled;
  }

  public boolean isParseBucketCalled() {
    return parseBucketCalled;
  }

  public boolean isLoadPoolCalled() {
    return loadPoolCalled;
  }

  public void setPoolName(String newPoolName) {
    this.poolName = newPoolName;
  }

  public void setPoolUri(String newPoolUri) {
    this.poolUri = newPoolUri;
  }

  public void setPoolStreamingUri(String newPoolStreamingUri) {
    this.poolStreamingUri = newPoolStreamingUri;
  }

  public void setBucketName(String newBucketName) {
    this.bucketName = newBucketName;
  }

  public void setVbuckets(DefaultConfig newVbuckets) {
    this.vbuckets = newVbuckets;
  }

  public void setBucketsUri(String newBucketsUri) {
    this.bucketsUri = newBucketsUri;
  }

  public void setBucketStreamingUri(String newBucketStreamingUri) {
    this.bucketStreamingUri = newBucketStreamingUri;
  }

  public void setNodes(List<Node> newNodes) {
    this.nodes = newNodes;
  }
}
