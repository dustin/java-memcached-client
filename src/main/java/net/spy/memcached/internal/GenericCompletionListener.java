/**
 * Copyright (C) 2006-2009 Dustin Sallings
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

package net.spy.memcached.internal;

import java.util.EventListener;
import java.util.concurrent.Future;

/**
 * A generic listener that will be notified once the future completes.
 *
 * <p>While this listener can be used directly, it is advised to subclass
 * it to make it easier for the API user to work with. See the
 * {@link OperationCompletionListener} as an example.</p>
 */
public interface GenericCompletionListener<F extends Future<?>>
  extends EventListener {

  /**
   * This method will be executed once the future completes.
   *
   * <p>Completion includes both failure and success, so it is advised to
   * always check the status and errors of the future.</p>
   *
   * @param future the future that got completed.
   * @throws Exception can potentially throw anything in the callback.
   */
  void onComplete(F future) throws Exception;

}
