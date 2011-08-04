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

package net.spy.memcached.transcoders;

import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Some test coverage for transcoder utils.
 */
public class TranscoderUtilsTest extends TestCase {

  private TranscoderUtils tu;
  private byte[] oversizeBytes = new byte[16];

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    tu = new TranscoderUtils(true);
  }

  public void testBooleanOverflow() {
    try {
      boolean b = tu.decodeBoolean(oversizeBytes);
      fail("Got " + b + " expected assertion.");
    } catch (AssertionError e) {
      // pass
    }
  }

  public void testByteOverflow() {
    try {
      byte b = tu.decodeByte(oversizeBytes);
      fail("Got " + b + " expected assertion.");
    } catch (AssertionError e) {
      // pass
    }
  }

  public void testIntOverflow() {
    try {
      int b = tu.decodeInt(oversizeBytes);
      fail("Got " + b + " expected assertion.");
    } catch (AssertionError e) {
      // pass
    }
  }

  public void testLongOverflow() {
    try {
      long b = tu.decodeLong(oversizeBytes);
      fail("Got " + b + " expected assertion.");
    } catch (AssertionError e) {
      // pass
    }
  }

  public void testPackedLong() {
    assertEquals("[1]", Arrays.toString(tu.encodeLong(1)));
  }

  public void testUnpackedLong() {
    assertEquals("[0, 0, 0, 0, 0, 0, 0, 1]",
        Arrays.toString(new TranscoderUtils(false).encodeLong(1)));
  }
}
