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

package net.spy.memcached.auth;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import net.spy.memcached.KeyUtil;
import net.spy.memcached.MemcachedConnection;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.OperationFactory;
import net.spy.memcached.compat.SpyThread;
import net.spy.memcached.compat.log.Level;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;

/**
 * A thread that does SASL authentication.
 */
public class AuthThread extends SpyThread {

  /**
   * If a SASL step takes longer than this period in milliseconds, a warning
   * will be issued instead of a debug message.
   */
  public static final int AUTH_ROUNDTRIP_THRESHOLD = 250;

  /**
   * If the total AUTH steps take longer than this period in milliseconds, a
   * warning will be issued instead of a debug message.
   */
  public static final int AUTH_TOTAL_THRESHOLD = 1000;

  public static final String MECH_SEPARATOR = " ";

  private final MemcachedConnection conn;
  private final AuthDescriptor authDescriptor;
  private final OperationFactory opFact;
  private final MemcachedNode node;

  public AuthThread(MemcachedConnection c, OperationFactory o,
      AuthDescriptor a, MemcachedNode n) {
    conn = c;
    opFact = o;
    authDescriptor = a;
    node = n;
    start();
  }

  protected String[] listSupportedSASLMechanisms(AtomicBoolean done) {
    final CountDownLatch listMechsLatch = new CountDownLatch(1);
    final AtomicReference<String> supportedMechs =
      new AtomicReference<String>();
    Operation listMechsOp = opFact.saslMechs(new OperationCallback() {
      @Override
      public void receivedStatus(OperationStatus status) {
        if(status.isSuccess()) {
          supportedMechs.set(status.getMessage());
          getLogger().debug("Received SASL supported mechs: "
            + status.getMessage());
        } else {
          getLogger().warn("Received non-success response for SASL mechs: "
            + status);
        }
      }

      @Override
      public void complete() {
        listMechsLatch.countDown();
      }

    });

    conn.insertOperation(node, listMechsOp);

    try {
      if (!conn.isShutDown()) {
        listMechsLatch.await();
      } else {
        done.set(true); // Connection is shutting down, tear.down.
      }
    } catch(InterruptedException ex) {
      getLogger().warn("Interrupted in Auth while waiting for SASL mechs.");
      // we can be interrupted if we were in the
      // process of auth'ing and the connection is
      // lost or dropped due to bad auth
      Thread.currentThread().interrupt();
      if (listMechsOp != null) {
        listMechsOp.cancel();
      }
      done.set(true); // If we were interrupted, tear down.
    }

    String supported = supportedMechs.get();
    if (supported == null || supported.isEmpty()) {
      return null;
    }
    return supported.split(MECH_SEPARATOR);
  }

  @Override
  public void run() {
    final AtomicBoolean done = new AtomicBoolean();
    long totalStart = System.nanoTime();

    String[] supportedMechs;
    long mechsStart = System.nanoTime();
    if (authDescriptor.getMechs() == null
      || authDescriptor.getMechs().length == 0) {
      supportedMechs = listSupportedSASLMechanisms(done);
    } else {
      supportedMechs = authDescriptor.getMechs();
    }
    long mechsDiff = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()
      - mechsStart);
    String msg = String.format("SASL List Mechanisms took %dms on %s",
      mechsDiff, node.toString());
    Level level = mechsDiff
      >= AUTH_ROUNDTRIP_THRESHOLD ? Level.WARN : Level.DEBUG;
    getLogger().log(level, msg);

    if (supportedMechs == null || supportedMechs.length == 0) {
      getLogger().warn("Authentication failed to " + node.getSocketAddress()
        + ", got empty SASL auth mech list.");
      throw new IllegalStateException("Got empty SASL auth mech list.");
    }

    OperationStatus priorStatus = null;
    while (!done.get()) {
      long stepStart = System.nanoTime();
      final CountDownLatch latch = new CountDownLatch(1);
      final AtomicReference<OperationStatus> foundStatus =
        new AtomicReference<OperationStatus>();

      final OperationCallback cb = new OperationCallback() {

        @Override
        public void receivedStatus(OperationStatus val) {
          // If the status we found was null, we're done.
          if (val.getMessage().length() == 0) {
            done.set(true);
            node.authComplete();
            getLogger().info("Authenticated to " + node.getSocketAddress());
          } else {
            foundStatus.set(val);
          }
        }

        @Override
        public void complete() {
          latch.countDown();
        }
      };

      // Get the prior status to create the correct operation.
      final Operation op = buildOperation(priorStatus, cb, supportedMechs);
      conn.insertOperation(node, op);

      try {
        if (!conn.isShutDown()) {
          latch.await();
        } else {
          done.set(true); // Connection is shutting down, tear.down.
        }
        Thread.sleep(100);
      } catch (InterruptedException e) {
        // we can be interrupted if we were in the
        // process of auth'ing and the connection is
        // lost or dropped due to bad auth
        Thread.currentThread().interrupt();
        if (op != null) {
          op.cancel();
        }
        done.set(true); // If we were interrupted, tear down.
      } finally {
        long stepDiff = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()
          - stepStart);
        msg = String.format("SASL Step took %dms on %s",
          stepDiff, node.toString());
        level = mechsDiff
          >= AUTH_ROUNDTRIP_THRESHOLD ? Level.WARN : Level.DEBUG;
        getLogger().log(level, msg);
      }

      // Get the new status to inspect it.
      priorStatus = foundStatus.get();
      if (priorStatus != null) {
        if (!priorStatus.isSuccess()) {
          getLogger().warn("Authentication failed to " + node.getSocketAddress()
            + ", Status: " + priorStatus);
        }
      }
    }

    long totalDiff = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()
      - totalStart);
    msg = String.format("SASL Auth took %dms on %s",
      totalDiff, node.toString());
    level = mechsDiff >= AUTH_TOTAL_THRESHOLD ? Level.WARN : Level.DEBUG;
    getLogger().log(level, msg);
  }

  private Operation buildOperation(OperationStatus st, OperationCallback cb,
    final String [] supportedMechs) {
    if (st == null) {
      return opFact.saslAuth(supportedMechs,
          node.getSocketAddress().toString(), null,
          authDescriptor.getCallback(), cb);
    } else {
      return opFact.saslStep(supportedMechs, KeyUtil.getKeyBytes(
          st.getMessage()), node.getSocketAddress().toString(), null,
          authDescriptor.getCallback(), cb);
    }
  }
}
