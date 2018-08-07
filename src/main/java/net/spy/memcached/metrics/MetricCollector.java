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
 * Defines a common API for all {@link MetricCollector}s.
 *
 * <p>The {@link MetricCollector} interface is used throughout the library,
 * independent of the actual underlying implementation. See the
 * {@link DefaultMetricCollector} for a real implementation and the
 * {@link NoopMetricCollector} for a discarding collector that has no
 * runtime overhead.</p>
 *
 * <p>Please note that the implementation is not expected to raise any kind
 * of error if the metric does not exist. To keep the actual application code
 * short, no extra checking needs to be done. If the counter has not been added
 * before, the value should be discarded. An error message can be logged though.
 * </p>
 */
public interface MetricCollector {

  /**
   * Add a Counter to the collector.
   *
   * @param name the name of the counter.
   */
  void addCounter(String name);

  /**
   * Remove a Counter from the collector.
   *
   * @param name the name of the counter.
   */
  void removeCounter(String name);

  /**
   * Increment a Counter by one.
   *
   * @param name the name of the counter.
   */
  void incrementCounter(String name);

  /**
   * Increment a Counter by the given amount.
   *
   * @param name the name of the counter.
   * @param amount the amount to increase.
   */
  void incrementCounter(String name, int amount);

  /**
   * Decrement a Counter by one.
   *
   * @param name the name of the counter.
   */
  void decrementCounter(String name);

  /**
   * Decrement a Counter by the given amount.
   *
   * @param name the name of the counter.
   * @param amount the amount to decrease.
   */
  void decrementCounter(String name, int amount);

  /**
   * Add a Meter to the Collector.
   *
   * @param name the name of the counter.
   */
  void addMeter(String name);

  /**
   * Remove a Meter from the Collector.
   *
   * @param name the name of the counter.
   */
  void removeMeter(String name);

  /**
   * Mark a checkpoint in the Meter.
   *
   * @param name the name of the counter.
   */
  void markMeter(String name);

  /**
   * Add a Histogram to the Collector.
   *
   * @param name the name of the counter.
   */
  void addHistogram(String name);

  /**
   * Remove a Histogram from the Collector.
   *
   * @param name the name of the counter.
   */
  void removeHistogram(String name);

  /**
   * Update the Histogram with the given amount.
   *
   * @param name the name of the counter.
   * @param amount the amount to update.
   */
  void updateHistogram(String name, int amount);

}
