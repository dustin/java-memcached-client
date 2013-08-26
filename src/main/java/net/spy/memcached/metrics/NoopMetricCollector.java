/**
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

/**
 * A {@link MetricCollector} that does nothing.
 *
 * This {@link MetricCollector} instance is used if Metric collection is
 * disabled during runtime. It just discards operations when executed.
 */
public final class NoopMetricCollector extends AbstractMetricCollector {

  @Override
  public void addCounter(String name) {
    return;
  }

  @Override
  public void removeCounter(String name) {
    return;
  }

  @Override
  public void incrementCounter(String name, int amount) {
    return;
  }

  @Override
  public void decrementCounter(String name, int amount) {
    return;
  }

  @Override
  public void addMeter(String name) {
    return;
  }

  @Override
  public void removeMeter(String name) {
    return;
  }

  @Override
  public void markMeter(String name) {
    return;
  }

  @Override
  public void addHistogram(String name) {
    return;
  }

  @Override
  public void removeHistogram(String name) {
    return;
  }

  @Override
  public void updateHistogram(String name, int amount) {
    return;
  }

}
