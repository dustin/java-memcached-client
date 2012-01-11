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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Test the hash algorithms.
 */
public class DefaultHashAlgorithmTest extends TestCase {

  private void assertHash(HashAlgorithm ha, String key, long exp) {
    assertTrue(exp >= 0L);
    // System.out.println(ha + "(" + key + ") = " + exp);
    assertEquals("Invalid " + ha + " for key ``" + key + "''", exp,
        ha.hash(key));
  }

  // I don't hardcode any values here because they're subject to change
  @edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_ABSOLUTE_VALUE_OF_HASHCODE")
  private void assertNativeHash(String key) {
    assertHash(DefaultHashAlgorithm.NATIVE_HASH, key, Math.abs(key.hashCode()));
  }

  public void testNativeHash() {
    for (String k : new String[] { "Test1", "Test2", "Test3", "Test4" }) {
      assertNativeHash(k);
    }
  }

  public void testCrc32Hash() {
    Map<String, Long> exp = new HashMap<String, Long>();
    exp.put("Test1", 19315L);
    exp.put("Test2", 21114L);
    exp.put("Test3", 9597L);
    exp.put("Test4", 15129L);
    exp.put("UDATA:edevil@sapo.pt", 558L);

    for (Map.Entry<String, Long> me : exp.entrySet()) {
      assertHash(DefaultHashAlgorithm.CRC_HASH, me.getKey(), me.getValue());
    }
  }

  public void testFNV164() {
    HashMap<String, Long> exp = new HashMap<String, Long>();
    exp.put("", 0x84222325L);
    exp.put(" ", 0x8601b7ffL);
    exp.put("hello world!", 0xb97b86bcL);
    exp.put("Lorem ipsum dolor sit amet, consectetuer adipiscing elit.",
        0xe87c054aL);
    exp.put("wd:com.google", 0x071b08f8L);
    exp.put("wd:com.google ", 0x12f03d48L);

    for (Map.Entry<String, Long> me : exp.entrySet()) {
      assertHash(DefaultHashAlgorithm.FNV1_64_HASH, me.getKey(),
          Math.abs(me.getValue()));
    }
  }

  // Thanks much to pierre@demartines.com for this unit test.
  public void testFNV1A64() {
    HashMap<String, Long> exp = new HashMap<String, Long>();
    exp.put("", 0x84222325L);
    exp.put(" ", 0x8601817fL);
    exp.put("hello world!", 0xcd5a2672L);
    exp.put("Lorem ipsum dolor sit amet, consectetuer adipiscing elit.",
        0xbec309a8L);
    exp.put("wd:com.google", 0x097b3f26L);
    exp.put("wd:com.google ", 0x1c6c1732L);

    for (Map.Entry<String, Long> me : exp.entrySet()) {
      assertHash(DefaultHashAlgorithm.FNV1A_64_HASH, me.getKey(),
          Math.abs(me.getValue()));
    }
  }

  public void testFNV132() {
    HashMap<String, Long> exp = new HashMap<String, Long>();
    exp.put("", 0x811c9dc5L);
    exp.put(" ", 0x050c5d3fL);
    exp.put("hello world!", 0x8a01b99cL);
    exp.put("Lorem ipsum dolor sit amet, consectetuer adipiscing elit.",
        0x9277524aL);
    exp.put("wd:com.google", 0x455e0df8L);
    exp.put("wd:com.google ", 0x2b0ffd48L);

    for (Map.Entry<String, Long> me : exp.entrySet()) {
      assertHash(DefaultHashAlgorithm.FNV1_32_HASH, me.getKey(),
          Math.abs(me.getValue()));
    }
  }

  public void testFNV1A32() {
    HashMap<String, Long> exp = new HashMap<String, Long>();
    exp.put("", 0x811c9dc5L);
    exp.put(" ", 0x250c8f7fL);
    exp.put("hello world!", 0xb034fff2L);
    exp.put("Lorem ipsum dolor sit amet, consectetuer adipiscing elit.",
        0xa9795ec8L);
    exp.put("wd:com.google", 0xaa90fcc6L);
    exp.put("wd:com.google ", 0x683e1e12L);

    for (Map.Entry<String, Long> me : exp.entrySet()) {
      assertHash(DefaultHashAlgorithm.FNV1A_32_HASH, me.getKey(),
          Math.abs(me.getValue()));
    }
  }

  // These values came from libketama's test prog.
  public void testKetamaHash() {
    HashMap<String, Long> exp = new HashMap<String, Long>();
    exp.put("26", 3979113294L);
    exp.put("1404", 2065000984L);
    exp.put("4177", 1125759251L);
    exp.put("9315", 3302915307L);
    exp.put("14745", 2580083742L);
    exp.put("105106", 3986458246L);
    exp.put("355107", 3611074310L);

    for (Map.Entry<String, Long> me : exp.entrySet()) {
      assertHash(DefaultHashAlgorithm.KETAMA_HASH, me.getKey(),
          Math.abs(me.getValue()));
    }
  }
}
