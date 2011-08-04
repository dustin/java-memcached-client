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

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.MemcachedClient;

/**
 * This expects a server on port 11212 that's somewhat unstable so it can report
 * and what-not.
 */
public final class ObserverToy {

  private ObserverToy() {
    // Empty
  }

  public static void main(String[] args) throws Exception {
    final ConnectionObserver obs = new ConnectionObserver() {
      public void connectionEstablished(SocketAddress sa, int reconnectCount) {
        System.out.println("*** Established:  " + sa + " count="
            + reconnectCount);
      }

      public void connectionLost(SocketAddress sa) {
        System.out.println("*** Lost connection:  " + sa);
      }

    };

    MemcachedClient c = new MemcachedClient(new DefaultConnectionFactory() {

      @Override
      public Collection<ConnectionObserver> getInitialObservers() {
        return Collections.singleton(obs);
      }

      @Override
      public boolean isDaemon() {
        return false;
      }

    }, AddrUtil.getAddresses("localhost:11212"));

    while (true) {
      c.waitForQueues(1, TimeUnit.SECONDS);
      Thread.sleep(1000);
    }
  }

}
