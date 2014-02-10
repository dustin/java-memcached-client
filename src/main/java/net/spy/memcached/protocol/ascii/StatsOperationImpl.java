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

package net.spy.memcached.protocol.ascii;

import java.nio.ByteBuffer;
import java.util.Arrays;

import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatsOperation;
import net.spy.memcached.ops.StatusCode;

/**
 * Operation to retrieve statistics from a memcached server.
 */
final class StatsOperationImpl extends OperationImpl implements StatsOperation {

  private static final OperationStatus END = new OperationStatus(true, "END",
    StatusCode.SUCCESS);

  private static final byte[] MSG = "stats\r\n".getBytes();

  private final byte[] msg;
  private final StatsOperation.Callback cb;

  public StatsOperationImpl(String arg, StatsOperation.Callback c) {
    super(c);
    cb = c;
    if (arg == null) {
      msg = MSG;
    } else {
      msg = ("stats " + arg + "\r\n").getBytes();
    }
  }

  @Override
  public void handleLine(String line) {
    if (line.equals("END")) {
      cb.receivedStatus(END);
      transitionState(OperationState.COMPLETE);
    } else {
      String[] parts = line.split(" ", 3);
      assert parts.length == 3;
      cb.gotStat(parts[1], parts[2]);
    }
  }

  @Override
  public void initialize() {
    setBuffer(ByteBuffer.wrap(msg));
  }

  @Override
  protected void wasCancelled() {
    cb.receivedStatus(CANCELLED);
  }

  @Override
  public String toString() {
    return "Cmd: " + Arrays.toString(msg);
  }
}
