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

package net.spy.memcached.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A future that fires immediately.
 */
public class ImmediateFuture implements Future<Boolean> {
  private final Boolean value;
  private final ExecutionException exception;

  public ImmediateFuture(Boolean returnValue) {
    value = returnValue;
    exception = null;
  }

  public ImmediateFuture(Exception e) {
    value = null;
    exception = new ExecutionException(e);
  }

  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  public Boolean get() throws InterruptedException, ExecutionException {
    if (exception != null) {
      throw exception;
    }
    return value;
  }

  public Boolean get(long timeout, TimeUnit unit) throws InterruptedException,
      ExecutionException, TimeoutException {
    if (exception != null) {
      throw exception;
    }
    return value;
  }

  public boolean isCancelled() {
    return false;
  }

  public boolean isDone() {
    return true;
  }
}
