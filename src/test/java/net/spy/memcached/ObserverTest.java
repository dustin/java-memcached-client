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

package net.spy.memcached;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.compat.SpyObject;

/**
 * Test observer hooks.
 */
public class ObserverTest extends ClientBaseCase {

  public void testConnectionObserver() throws Exception {
    ConnectionObserver obs = new LoggingObserver();
    assertTrue("Didn't add observer.", client.addObserver(obs));
    assertTrue("Didn't remove observer.", client.removeObserver(obs));
    assertFalse("Removed observer more than once.", client.removeObserver(obs));
  }

  public void testInitialObservers() throws Exception {
    assertTrue("Couldn't shut down within five seconds",
        client.shutdown(5, TimeUnit.SECONDS));

    final CountDownLatch latch = new CountDownLatch(1);
    final ConnectionObserver obs = new ConnectionObserver() {

      public void connectionEstablished(SocketAddress sa, int reconnectCount) {
        latch.countDown();
      }

      public void connectionLost(SocketAddress sa) {
        assert false : "Should not see this.";
      }

    };

    // Get a new client
    initClient(new DefaultConnectionFactory() {

      @Override
      public Collection<ConnectionObserver> getInitialObservers() {
        return Collections.singleton(obs);
      }

    });

    assertTrue("Didn't detect connection", latch.await(2, TimeUnit.SECONDS));
    assertTrue("Did not install observer.", client.removeObserver(obs));
    assertFalse("Didn't clean up observer.", client.removeObserver(obs));
  }

  static class LoggingObserver extends SpyObject implements ConnectionObserver {
    public void connectionEstablished(SocketAddress sa, int reconnectCount) {
      getLogger().info("Connection established to %s (%s)", sa, reconnectCount);
    }

    public void connectionLost(SocketAddress sa) {
      getLogger().info("Connection lost from %s", sa);
    }
  }
}
