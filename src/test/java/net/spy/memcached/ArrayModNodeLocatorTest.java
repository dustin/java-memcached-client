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

package net.spy.memcached;

import java.util.Arrays;
import java.util.Collection;

/**
 * Test the ArrayModNodeLocator.
 */
public class ArrayModNodeLocatorTest extends AbstractNodeLocationCase {

  @Override
  protected void setupNodes(int n) {
    super.setupNodes(n);
    locator = new ArrayModNodeLocator(Arrays.asList(nodes),
        DefaultHashAlgorithm.NATIVE_HASH);
  }

  public void testPrimary() throws Exception {
    setupNodes(4);
    assertSame(nodes[3], locator.getPrimary("dustin"));
    assertSame(nodes[0], locator.getPrimary("x"));
    assertSame(nodes[1], locator.getPrimary("y"));
  }

  public void testPrimaryClone() throws Exception {
    setupNodes(4);
    assertEquals(nodes[3].toString(),
        locator.getReadonlyCopy().getPrimary("dustin").toString());
    assertEquals(nodes[0].toString(), locator.getReadonlyCopy().getPrimary("x")
        .toString());
    assertEquals(nodes[1].toString(), locator.getReadonlyCopy().getPrimary("y")
        .toString());
  }

  public void testAll() throws Exception {
    setupNodes(4);
    Collection<MemcachedNode> all = locator.getAll();
    assertEquals(4, all.size());
    assertTrue(all.contains(nodes[0]));
    assertTrue(all.contains(nodes[1]));
    assertTrue(all.contains(nodes[2]));
    assertTrue(all.contains(nodes[3]));
  }

  public void testAllClone() throws Exception {
    setupNodes(4);
    Collection<MemcachedNode> all = locator.getReadonlyCopy().getAll();
    assertEquals(4, all.size());
  }

  public void testSeq1() {
    setupNodes(4);
    assertSequence("dustin", 0, 1, 2);
  }

  public void testSeq2() {
    setupNodes(4);
    assertSequence("noelani", 1, 2, 3);
  }

  public void testSeqOnlyOneServer() {
    setupNodes(1);
    assertSequence("noelani");
  }

  public void testSeqWithTwoNodes() {
    setupNodes(2);
    assertSequence("dustin", 0);
  }
}
