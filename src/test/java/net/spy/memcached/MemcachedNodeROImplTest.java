/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2011 Couchbase, Inc.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

/**
 * Test readonliness of the MemcachedNodeROImpl.
 */
public class MemcachedNodeROImplTest extends MockObjectTestCase {

  public void testReadOnliness() throws Exception {
    SocketAddress sa = new InetSocketAddress(11211);
    Mock m = mock(MemcachedNode.class, "node");
    MemcachedNodeROImpl node =
        new MemcachedNodeROImpl((MemcachedNode) m.proxy());
    m.expects(once()).method("getSocketAddress").will(returnValue(sa));

    assertSame(sa, node.getSocketAddress());
    assertEquals(m.proxy().toString(), node.toString());

    Set<String> acceptable = new HashSet<String>(Arrays.asList("toString",
        "getSocketAddress", "getBytesRemainingToWrite", "getReconnectCount",
        "getSelectionOps", "hasReadOp", "hasWriteOp", "isActive"));

    for (Method meth : MemcachedNode.class.getMethods()) {
      if (acceptable.contains(meth.getName())) {
        return;
      } else {
        Object[] args = new Object[meth.getParameterTypes().length];
        fillArgs(meth.getParameterTypes(), args);
        try {
          meth.invoke(node, args);
          fail("Failed to break on " + meth.getName());
        } catch (InvocationTargetException e) {
          assertSame("Fail at " + meth.getName(),
              UnsupportedOperationException.class, e.getCause().getClass());
        }
      }
    }
  }

  private void fillArgs(Class<?>[] parameterTypes, Object[] args) {
    int i = 0;
    for (Class<?> c : parameterTypes) {
      if (c == Boolean.TYPE) {
        args[i++] = false;
      } else {
        args[i++] = null;
      }
    }
  }
}
