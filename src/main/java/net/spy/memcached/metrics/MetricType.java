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
 * Defines the type of metric collection to use.
 *
 * More detailed types provide more insight, but can come with more
 * overhead during collection.
 */
public enum MetricType {

  /**
   * No metrics collection.
   *
   * If the "OFF" type is chosen, no metrics will be registered
   * and collected.
   */
  OFF,

  /**
   * Metrics useful for performance-related tracing.
   *
   * These metrics provide insight into the application performance
   * and show how the operations flow in and out of the library.
   */
  PERFORMANCE,

  /**
   * Metrics useful for debugging.
   *
   * These metrics (which include the PERFORMANCE metrics implicitly) provide
   * more insight into the state of the library (for example node states),
   * but it comes with larger aggregation overhead. Use during development
   * and debug sessions.
   */
  DEBUG
}
