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

import junit.framework.TestCase;
import net.spy.memcached.CachedData;

/**
 * Test a couple aspects of CachedData.
 */
public class CachedDataTest extends TestCase {

  public void testToString() throws Exception {
    String exp = "{CachedData flags=13 data=[84, 104, 105, 115, 32, 105, "
        + "115, 32, 97, 32, 115, 105, 109, 112, 108, 101, 32, 116, 101, "
        + "115, 116, 32, 115, 116, 114, 105, 110, 103, 46]}";
    CachedData cd =
        new CachedData(13, "This is a simple test string.".getBytes("UTF-8"),
        CachedData.MAX_SIZE);
    assertEquals(exp, String.valueOf(cd));
  }
}
