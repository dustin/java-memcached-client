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

import java.net.URL;
import java.util.List;

import net.spy.memcached.HashAlgorithm;

/**
 * A Config.
 */
public interface Config {

  // Config access

  int getReplicasCount();

  int getVbucketsCount();

  int getServersCount();

  HashAlgorithm getHashAlgorithm();

  String getServer(int serverIndex);

  // VBucket access

  int getVbucketByKey(String key);

  int getMaster(int vbucketIndex);

  int getReplica(int vbucketIndex, int replicaIndex);

  int foundIncorrectMaster(int vbucket, int wrongServer);

  ConfigDifference compareTo(Config config);

  List<String> getServers();

  List<URL> getCouchServers();

  List<VBucket> getVbuckets();

  ConfigType getConfigType();
}
