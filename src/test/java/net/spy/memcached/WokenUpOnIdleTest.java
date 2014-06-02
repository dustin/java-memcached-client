/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2014 Couchbase, Inc.
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

import net.spy.memcached.protocol.binary.BinaryOperationFactory;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Verifies the functionality of the {@link MemcachedConnection} that the
 * selector gets woken up automatically if idle.
 */
public class WokenUpOnIdleTest {

  @Test
  public void shouldWakeUpOnIdle() throws Exception {
    CountDownLatch latch = new CountDownLatch(3);
    MemcachedConnection connection = new InstrumentedConnection(
      latch,
      1024,
      new BinaryConnectionFactory(),
      Arrays.asList(new InetSocketAddress(11210)),
      Collections.<ConnectionObserver>emptyList(),
      FailureMode.Redistribute,
      new BinaryOperationFactory()
    );

    assertTrue(latch.await(5, TimeUnit.SECONDS));
  }

  static class InstrumentedConnection extends MemcachedConnection {
    final CountDownLatch latch;
    InstrumentedConnection(CountDownLatch latch, int bufSize, ConnectionFactory f,
      List<InetSocketAddress> a, Collection<ConnectionObserver> obs,
      FailureMode fm, OperationFactory opfactory) throws IOException {
      super(bufSize, f, a, obs, fm, opfactory);
      this.latch = latch;
    }

    @Override
    protected void handleWokenUpSelector() {
      latch.countDown();
    }
  }

}
