/**
 * Copyright (C) 2006-2009 Dustin Sallings
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

package net.spy.memcached.test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.MemcachedClient;

/**
 * Adaptation of http://code.google.com/p/spcached/wiki/benchmarktool.
 */
public final class MemcachedThreadBench extends TestCase {

  private MemcachedThreadBench() {
    // Empty
  }

  private static final class WorkerStat {
    private int start, runs;

    private long setterTime, getterTime;

    protected WorkerStat() {
      start = runs = 0;
      setterTime = getterTime = 0;
    }
  }

  public void testCrap() throws Exception {
    main(new String[] { "10000", "100", "11211", "100" });
  }

  public static void main(String[] args) throws Exception {

    if (args.length != 4) {
      args = new String[] { "1000", "100", "11211", "100" };
      System.out.println("Usage: java " + MemcachedThreadBench.class.getName()
          + " <runs> <start> <port> <threads>");
    }

    int runs = Integer.parseInt(args[0]);
    int start = Integer.parseInt(args[1]);
    String serverlist = "127.0.0.1:" + args[2];
    int threads = Integer.parseInt(args[3]);

    MemcachedClient client =
        new MemcachedClient(new DefaultConnectionFactory(100000, 32768),
            AddrUtil.getAddresses(serverlist));

    WorkerStat[] statArray = new WorkerStat[threads];
    Thread[] threadArray = new Thread[threads];

    WorkerStat mainStat = new WorkerStat();
    mainStat.runs = runs * threads;

    long begin = System.currentTimeMillis();

    for (int i = 0; i < threads; i++) {
      statArray[i] = new WorkerStat();
      statArray[i].start = start + i * runs;
      statArray[i].runs = runs;
      threadArray[i] = new SetterThread(client, statArray[i]);
      threadArray[i].start();
    }

    for (int i = 0; i < threads; i++) {
      threadArray[i].join();
    }

    mainStat.setterTime = System.currentTimeMillis() - begin;

    begin = System.currentTimeMillis();

    for (int i = 0; i < threads; i++) {
      threadArray[i] = new GetterThread(client, statArray[i]);
      threadArray[i].start();
    }

    for (int i = 0; i < threads; i++) {
      threadArray[i].join();
    }

    mainStat.getterTime = System.currentTimeMillis() - begin;

    client.shutdown();

    WorkerStat totalStat = new WorkerStat();

    System.out.println("Thread\tstart\truns\tset time(ms)\tget time(ms)");
    for (int i = 0; i < threads; i++) {
      System.out.println("" + i + "\t" + statArray[i].start + "\t"
          + statArray[i].runs + "\t" + statArray[i].setterTime + "\t\t"
          + statArray[i].getterTime);

      totalStat.runs = totalStat.runs + statArray[i].runs;
      totalStat.setterTime = totalStat.setterTime + statArray[i].setterTime;
      totalStat.getterTime = totalStat.getterTime + statArray[i].getterTime;
    }

    System.out.println("\nAvg\t\t" + runs + "\t" + totalStat.setterTime
        / threads + "\t\t" + totalStat.getterTime / threads);

    System.out.println("\nTotal\t\t" + totalStat.runs + "\t"
        + totalStat.setterTime + "\t\t" + totalStat.getterTime);
    System.out.println("\tReqPerSecond\tset - " + 1000 * totalStat.runs
        / totalStat.setterTime + "\tget - " + 1000 * totalStat.runs
        / totalStat.getterTime);

    System.out.println("\nMain\t\t" + mainStat.runs + "\t"
        + mainStat.setterTime + "\t\t" + mainStat.getterTime);
    System.out.println("\tReqPerSecond\tset - " + 1000 * mainStat.runs
        / mainStat.setterTime + "\tget - " + 1000 * mainStat.runs
        / mainStat.getterTime);
  }

  private static class SetterThread extends Thread {
    private static final AtomicInteger TOTAL = new AtomicInteger(0);
    private static final int MAX_QUEUE = 10000;
    private final MemcachedClient mc;
    private final WorkerStat stat;

    SetterThread(MemcachedClient c, WorkerStat st) {
      stat = st;
      mc = c;
    }

    @Override
    public void run() {
      String keyBase = "testKey";
      String object =
          "This is a test of an object blah blah es, "
              + "serialization does not seem to slow things down so much.  "
              + "The gzip compression is horrible horrible performance, "
              + "so we only use it for very large objects.  "
              + "I have not done any heavy benchmarking recently";

      long begin = System.currentTimeMillis();
      for (int i = stat.start; i < stat.start + stat.runs; i++) {
        mc.set("" + i + keyBase, 3600, object);
        if (TOTAL.incrementAndGet() >= MAX_QUEUE) {
          flush();
        }
      }
      long end = System.currentTimeMillis();

      stat.setterTime = end - begin;
    }

    private synchronized void flush() {
      if (TOTAL.intValue() >= MAX_QUEUE) {
        mc.waitForQueues(5, TimeUnit.SECONDS);
        TOTAL.set(0);
      }
    }
  }

  private static class GetterThread extends Thread {
    private final MemcachedClient mc;
    private final WorkerStat stat;

    GetterThread(MemcachedClient c, WorkerStat st) {
      stat = st;
      mc = c;
    }

    @Override
    public void run() {
      String keyBase = "testKey";

      long begin = System.currentTimeMillis();
      for (int i = stat.start; i < stat.start + stat.runs; i++) {
        String str = (String) mc.get("" + i + keyBase);
        assert str != null;
      }
      long end = System.currentTimeMillis();

      stat.getterTime = end - begin;
    }
  }
}
