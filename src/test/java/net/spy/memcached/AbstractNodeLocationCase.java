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

import java.util.Arrays;
import java.util.Iterator;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

/**
 * An AbstractNodeLocationCase.
 */
public abstract class AbstractNodeLocationCase extends MockObjectTestCase {

  protected MemcachedNode[] nodes;
  protected Mock[] nodeMocks;
  protected NodeLocator locator;

  private void runSequenceAssertion(NodeLocator l, String k, int... seq) {
    int pos = 0;
    for (Iterator<MemcachedNode> i = l.getSequence(k); i.hasNext();) {
      String loc = i.next().toString();
      try {
        assertEquals("At position " + pos, nodes[seq[pos]].toString(), loc);
        i.remove();
        fail("Allowed a removal from a sequence.");
      } catch (UnsupportedOperationException e) {
        // pass
      } catch (ArrayIndexOutOfBoundsException ex) {
        throw new RuntimeException("Tried to access nodes[" + Arrays.toString(seq) + "[" + pos
          + "]] erroneously.", ex);
      }
      pos++;
    }
    assertEquals("Incorrect sequence size for " + k, seq.length, pos);
  }

  public final void testCloningGetPrimary() {
    setupNodes(5);
    assertTrue(locator.getReadonlyCopy().getPrimary("hi")
      instanceof MemcachedNodeROImpl);
  }

  public final void testCloningGetAll() {
    setupNodes(5);
    assertTrue(locator.getReadonlyCopy().getAll().iterator().next()
      instanceof MemcachedNodeROImpl);
  }

  public final void testCloningGetSequence() {
    setupNodes(5);
    assertTrue(locator.getReadonlyCopy().getSequence("hi").next()
      instanceof MemcachedNodeROImpl);
  }

  protected final void assertSequence(String k, int... seq) {
    runSequenceAssertion(locator, k, seq);
    runSequenceAssertion(locator.getReadonlyCopy(), k, seq);
  }

  protected void setupNodes(int n) {
    nodes = new MemcachedNode[n];
    nodeMocks = new Mock[nodes.length];

    for (int i = 0; i < nodeMocks.length; i++) {
      nodeMocks[i] = mock(MemcachedNode.class, "node#" + i);
      nodes[i] = (MemcachedNode) nodeMocks[i].proxy();
    }
  }
}
