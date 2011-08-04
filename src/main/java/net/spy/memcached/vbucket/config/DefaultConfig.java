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

import java.util.List;

import net.spy.memcached.HashAlgorithm;

/**
 * A DefaultConfig.
 */
public class DefaultConfig implements Config {

  private final HashAlgorithm hashAlgorithm;

  private final int vbucketsCount;

  private final int mask;

  private final int serversCount;

  private final int replicasCount;

  private final List<String> servers;

  private final List<VBucket> vbuckets;

  public DefaultConfig(HashAlgorithm hashAlgorithm, int serversCount,
      int replicasCount, int vbucketsCount, List<String> servers,
      List<VBucket> vbuckets) {
    this.hashAlgorithm = hashAlgorithm;
    this.serversCount = serversCount;
    this.replicasCount = replicasCount;
    this.vbucketsCount = vbucketsCount;
    this.mask = vbucketsCount - 1;
    this.servers = servers;
    this.vbuckets = vbuckets;
  }

  @Override
  public int getReplicasCount() {
    return replicasCount;
  }

  @Override
  public int getVbucketsCount() {
    return vbucketsCount;
  }

  @Override
  public int getServersCount() {
    return serversCount;
  }

  @Override
  public String getServer(int serverIndex) {
    return servers.get(serverIndex);
  }

  @Override
  public int getVbucketByKey(String key) {
    int digest = (int) hashAlgorithm.hash(key);
    return digest & mask;
  }

  @Override
  public int getMaster(int vbucketIndex) {
    return vbuckets.get(vbucketIndex).getMaster();
  }

  @Override
  public int getReplica(int vbucketIndex, int replicaIndex) {
    return vbuckets.get(vbucketIndex).getReplica(replicaIndex);
  }

  @Override
  public int foundIncorrectMaster(int vbucket, int wrongServer) {
    int mappedServer = this.vbuckets.get(vbucket).getMaster();
    int rv = mappedServer;
    if (mappedServer == wrongServer) {
      rv = (rv + 1) % this.serversCount;
      this.vbuckets.get(vbucket).setMaster(rv);
    }
    return rv;
  }

  @Override
  public List<String> getServers() {
    return servers;
  }

  @Override
  public List<VBucket> getVbuckets() {
    return vbuckets;
  }

  @Override
  public ConfigDifference compareTo(Config config) {
    ConfigDifference difference = new ConfigDifference();

    // Verify the servers are equal in their positions
    if (this.serversCount == config.getServersCount()) {
      difference.setSequenceChanged(false);
      for (int i = 0; i < this.serversCount; i++) {
        if (!this.getServer(i).equals(config.getServer(i))) {
          difference.setSequenceChanged(true);
          break;
        }
      }
    } else {
      // Just say yes
      difference.setSequenceChanged(true);
    }

    // Count the number of vbucket differences
    if (this.vbucketsCount == config.getVbucketsCount()) {
      int vbucketsChanges = 0;
      for (int i = 0; i < this.vbucketsCount; i++) {
        vbucketsChanges += (this.getMaster(i) == config.getMaster(i)) ? 0 : 1;
      }
      difference.setVbucketsChanges(vbucketsChanges);
    } else {
      difference.setVbucketsChanges(-1);
    }

    return difference;
  }

  @Override
  public HashAlgorithm getHashAlgorithm() {
    return hashAlgorithm;
  }

  public ConfigType getConfigType() {
    return ConfigType.MEMBASE;
  }
}
