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

import java.util.concurrent.Callable;

import net.spy.memcached.compat.SyncThread;
import net.spy.memcached.transcoders.LongTranscoder;

/**
 * Test the CAS mutator.
 */
public class CASMutatorTest extends ClientBaseCase {

  private CASMutation<Long> mutation;
  private CASMutator<Long> mutator;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mutation = new CASMutation<Long>() {
      public Long getNewValue(Long current) {
        return current + 1;
      }
    };
    mutator = new CASMutator<Long>(client, new LongTranscoder(), 50);
  }

  public void testDefaultConstructor() {
    // Just validate that this doesn't throw an exception.
    new CASMutator<Long>(client, new LongTranscoder());
  }

  public void testConcurrentCAS() throws Throwable {
    int num = SyncThread.getDistinctResultCount(20, new Callable<Long>() {
      public Long call() throws Exception {
        return mutator.cas("test.cas.concurrent", 0L, 0, mutation);
      }
    });
    assertEquals(20, num);
  }

  public void testIncorrectTypeInCAS() throws Throwable {
    // Stick something for this CAS in the cache.
    client.set("x", 0, "not a long");
    try {
      Long rv = mutator.cas("x", 1L, 0, mutation);
      fail("Expected RuntimeException on invalid type mutation, got " + rv);
    } catch (RuntimeException e) {
      assertEquals("Couldn't get a CAS in 50 attempts", e.getMessage());
    }
  }

  public void testCASUpdateWithNullInitial() throws Throwable {
    client.set("x", 0, 1L);
    Long rv = mutator.cas("x", (Long) null, 0, mutation);
    assertEquals(rv, (Long) 2L);
  }

  public void testCASUpdateWithNullInitialNoExistingVal() throws Throwable {
    assertNull(client.get("x"));
    Long rv = mutator.cas("x", (Long) null, 0, mutation);
    assertNull(rv);
    assertNull(client.get("x"));
  }

  public void testCASValueToString() {
    CASValue<String> c = new CASValue<String>(717L, "hi");
    assertEquals("{CasValue 717/hi}", c.toString());
  }
}
