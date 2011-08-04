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

/**
 * A ConfigDifference.
 */
public class ConfigDifference {

  /**
   * List of server names that were added.
   */
  private List<String> serversAdded;

  /**
   * List of server names that were removed.
   */
  private List<String> serversRemoved;

  /**
   * Number of vbuckets that changed. -1 if the total number changed.
   */
  private int vbucketsChanges;

  /**
   * True if the sequence of servers changed.
   */
  private boolean sequenceChanged;

  public List<String> getServersAdded() {
    return serversAdded;
  }

  protected void setServersAdded(List<String> newServersAdded) {
    this.serversAdded = newServersAdded;
  }

  public List<String> getServersRemoved() {
    return serversRemoved;
  }

  protected void setServersRemoved(List<String> newServersRemoved) {
    this.serversRemoved = newServersRemoved;
  }

  public int getVbucketsChanges() {
    return vbucketsChanges;
  }

  protected void setVbucketsChanges(int newVbucketsChanges) {
    this.vbucketsChanges = newVbucketsChanges;
  }

  public boolean isSequenceChanged() {
    return sequenceChanged;
  }

  protected void setSequenceChanged(boolean newSequenceChanged) {
    this.sequenceChanged = newSequenceChanged;
  }
}
