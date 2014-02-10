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

package net.spy.memcached.protocol;

import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatusCode;

/**
 * Wrapper callback for use in optimized gets.
 */
public class GetCallbackWrapper implements GetOperation.Callback {

  private static final OperationStatus END = new OperationStatus(true, "END",
    StatusCode.SUCCESS);

  private boolean completed = false;
  private int remainingKeys = 0;
  private GetOperation.Callback cb = null;

  public GetCallbackWrapper(int k, GetOperation.Callback c) {
    super();
    remainingKeys = k;
    cb = c;
  }

  public void gotData(String key, int flags, byte[] data) {
    assert !completed : "Got data for a completed wrapped op";
    cb.gotData(key, flags, data);
    if (--remainingKeys == 0) {
      // Fake a status line
      receivedStatus(END);
    }
  }

  public void receivedStatus(OperationStatus status) {
    if (!completed) {
      cb.receivedStatus(status);
    }
  }

  public void complete() {
    assert !completed;
    cb.complete();
    completed = true;
  }
}
