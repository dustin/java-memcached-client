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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.OperationStatus;

/**
 * Proxy callback used for dispatching callbacks over optimized gets.
 */
public class ProxyCallback implements GetOperation.Callback {

  private final Map<String, Collection<GetOperation.Callback>> callbacks =
      new HashMap<String, Collection<GetOperation.Callback>>();
  private final Collection<GetOperation.Callback> allCallbacks =
      new ArrayList<GetOperation.Callback>();

  public void addCallbacks(GetOperation o) {
    GetOperation.Callback c =
        new GetCallbackWrapper(o.getKeys().size(),
            (GetOperation.Callback) o.getCallback());
    allCallbacks.add(c);
    for (String s : o.getKeys()) {
      Collection<GetOperation.Callback> cbs = callbacks.get(s);
      if (cbs == null) {
        cbs = new ArrayList<GetOperation.Callback>();
        callbacks.put(s, cbs);
      }
      cbs.add(c);
    }
  }

  public void gotData(String key, int flags, byte[] data) {
    Collection<GetOperation.Callback> cbs = callbacks.get(key);
    assert cbs != null : "No callbacks for key " + key;
    for (GetOperation.Callback c : cbs) {
      c.gotData(key, flags, data);
    }
  }

  public void receivedStatus(OperationStatus status) {
    for (GetOperation.Callback c : allCallbacks) {
      c.receivedStatus(status);
    }
  }

  public void complete() {
    for (GetOperation.Callback c : allCallbacks) {
      c.complete();
    }
  }

  public int numKeys() {
    return callbacks.size();
  }

  public int numCallbacks() {
    return allCallbacks.size();
  }
}
