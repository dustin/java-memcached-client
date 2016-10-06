/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2013 Couchbase, Inc.
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

package net.spy.memcached.metrics;

import java.util.HashMap;

import net.spy.memcached.MemcachedNode;

/**
 * A dummy {@link MetricCollector} to measure executions.
 */
public class DummyMetricCollector implements MetricCollector {

  private HashMap<String, Integer> metrics;

  public DummyMetricCollector() {
    metrics = new HashMap<String, Integer>();
  }

  @Override
  public MetricCollector forNode(MemcachedNode node) {
    return this;
  }

  @Override
  public void addCounter(String name) {
    metrics.put(name, 0);
  }

  @Override
  public void removeCounter(String name) {
    metrics.remove(name);
  }

  @Override
  public void incrementCounter(String name) {
    metrics.put(name, metrics.get(name) + 1);
  }

  @Override
  public void incrementCounter(String name, int amount) {
    metrics.put(name, metrics.get(name) + amount);
  }

  @Override
  public void decrementCounter(String name) {
    metrics.put(name, metrics.get(name) - 1);
  }

  @Override
  public void decrementCounter(String name, int amount) {
    metrics.put(name, metrics.get(name) - amount);
  }

  @Override
  public void addMeter(String name) {
    metrics.put(name, 0);
  }

  @Override
  public void removeMeter(String name) {
    metrics.remove(name);
  }

  @Override
  public void markMeter(String name) {
    metrics.put(name, metrics.get(name) + 1);
  }

  @Override
  public void addHistogram(String name) {
    metrics.put(name, 0);
  }

  @Override
  public void removeHistogram(String name) {
    metrics.remove(name);
  }

  @Override
  public void updateHistogram(String name, int amount) {
    metrics.put(name, metrics.get(name) + amount);
  }

  public HashMap<String, Integer> getMetrics() {
    return metrics;
  }

}
