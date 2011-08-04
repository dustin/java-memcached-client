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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.ops.Operation;

/**
 * Timeout exception that tracks the original operation.
 */
public class CheckedOperationTimeoutException extends TimeoutException {

  private static final long serialVersionUID = 139074906690883031L;

  private final Collection<Operation> operations;

  /**
   * Construct a CheckedOperationTimeoutException with the given message and
   * operation.
   *
   * @param message the message
   * @param op the operation that timed out
   */
  public CheckedOperationTimeoutException(String message, Operation op) {
    this(message, Collections.singleton(op));
  }

  public CheckedOperationTimeoutException(String message,
      Collection<Operation> ops) {
    super(createMessage(message, ops));
    operations = ops;
  }

  private static String
  createMessage(String message, Collection<Operation> ops) {
    StringBuilder rv = new StringBuilder(message);
    rv.append(" - failing node");
    rv.append(ops.size() == 1 ? ": " : "s: ");
    boolean first = true;
    for (Operation op : ops) {
      if (first) {
        first = false;
      } else {
        rv.append(", ");
      }
      MemcachedNode node = op == null ? null : op.getHandlingNode();
      rv.append(node == null ? "<unknown>" : node.getSocketAddress());
    }
    return rv.toString();
  }

  /**
   * Get the operation that timed out.
   */
  public Collection<Operation> getOperations() {
    return operations;
  }
}
