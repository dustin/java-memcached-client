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

import com.codahale.metrics.*;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * A {@link MetricCollector} that uses the Codahale Metrics library.
 *
 * The following system properies can be used to customize the behavior
 * of the collector during runtime:
 * <pre>
 * - net.spy.metrics.reporter.type = console (console/jmx/csv/slf4j)
 * - net.spy.metrics.reporter.interval = 30 (time interval to log)
 * - net.spy.metrics.reporter.outdir = ~/mydir (output dir for csv reporter)
 * </pre>
 */
public final class DefaultMetricCollector extends AbstractMetricCollector {

  /**
   * Use the "console" reporter by default.
   */
  public static final String DEFAULT_REPORTER_TYPE = "console";

  /**
   * Log every 30 seconds to the console by default.
   */
  public static final String DEFAULT_REPORTER_INTERVAL = "30";

  /**
   * Define an empty directory for the CSV exporter by default.
   */
  public static final String DEFAULT_REPORTER_OUTDIR = "";

  /**
   * Holds the registry where all metrics are stored.
   */
  private MetricRegistry registry;

  /**
   * Contains all registered {@link Counter}s.
   */
  private ConcurrentHashMap<String, Counter> counters;

  /**
   * Contains all registered {@link Meter}s.
   */
  private ConcurrentHashMap<String, Meter> meters;

  /**
   * Contains all registered {@link Histogram}s.
   */
  private ConcurrentHashMap<String, Histogram> histograms;

  /**
   * Create a new {@link DefaultMetricCollector}.
   *
   * Note that when this constructor is called, the reporter is also
   * automatically established.
   */
  public DefaultMetricCollector() {
    registry = new MetricRegistry();
    counters = new ConcurrentHashMap<String, Counter>();
    meters = new ConcurrentHashMap<String, Meter>();
    histograms = new ConcurrentHashMap<String, Histogram>();

    initReporter();
  }

  /**
   * Initialize the proper metrics Reporter.
   */
  private void initReporter() {
    String reporterType =
      System.getProperty("net.spy.metrics.reporter.type", DEFAULT_REPORTER_TYPE);
    String reporterInterval =
      System.getProperty("net.spy.metrics.reporter.interval", DEFAULT_REPORTER_INTERVAL);
    String reporterDir =
      System.getProperty("net.spy.metrics.reporter.outdir", DEFAULT_REPORTER_OUTDIR);

    if(reporterType.equals("console")) {
      final ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.SECONDS)
        .build();
      reporter.start(Integer.parseInt(reporterInterval), TimeUnit.SECONDS);
    } else if (reporterType.equals("jmx")) {
      final JmxReporter reporter = JmxReporter.forRegistry(registry)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.SECONDS)
        .build();
      reporter.start();
    } else if (reporterType.equals("csv")) {
      final CsvReporter reporter = CsvReporter.forRegistry(registry)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.SECONDS)
        .build(new File(reporterDir));
      reporter.start(Integer.parseInt(reporterInterval), TimeUnit.SECONDS);
    } else if (reporterType.equals("slf4j")) {
      final Slf4jReporter reporter = Slf4jReporter.forRegistry(registry)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.SECONDS)
        .outputTo(LoggerFactory.getLogger(MetricCollector.class))
        .build();
      reporter.start(Integer.parseInt(reporterInterval), TimeUnit.SECONDS);
    } else {
        throw new IllegalStateException("Unknown Metrics Reporter Type: " + reporterType);
    }
  }

  @Override
  public void addCounter(String name) {
    if (!counters.containsKey(name)) {
      counters.put(name, registry.counter(name));
    }
  }

  @Override
  public void removeCounter(String name) {
    if (!counters.containsKey(name)) {
      registry.remove(name);
      counters.remove(name);
    }
  }

  @Override
  public void incrementCounter(String name, int amount) {
    if (counters.containsKey(name)) {
      counters.get(name).inc(amount);
    }
  }

  @Override
  public void decrementCounter(String name, int amount) {
    if (counters.containsKey(name)) {
      counters.get(name).dec(amount);
    }
  }

  @Override
  public void addMeter(String name) {
    if (!meters.containsKey(name)) {
      meters.put(name, registry.meter(name));
    }
  }

  @Override
  public void removeMeter(String name) {
    if (meters.containsKey(name)) {
      meters.remove(name);
    }
  }

  @Override
  public void markMeter(String name) {
    if (meters.containsKey(name)) {
      meters.get(name).mark();
    }
  }

  @Override
  public void addHistogram(String name) {
    if (!histograms.containsKey(name)) {
      histograms.put(name, registry.histogram(name));
    }
  }

  @Override
  public void removeHistogram(String name) {
    if (histograms.containsKey(name)) {
      histograms.remove(name);
    }
  }

  @Override
  public void updateHistogram(String name, int amount) {
    if (histograms.containsKey(name)) {
      histograms.get(name).update(amount);
    }
  }
}
