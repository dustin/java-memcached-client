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
import java.util.Calendar;

import net.spy.memcached.CachedData;

/**
 * Test the serializing transcoder.
 */
public class WhalinTranscoderTest extends BaseTranscoderCase {

  private WhalinTranscoder tc;
  private TranscoderUtils tu;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    tc = new WhalinTranscoder();
    setTranscoder(tc);
    tu = new TranscoderUtils(false);
  }

  public void testNonserializable() throws Exception {
    try {
      tc.encode(new Object());
      fail("Processed a non-serializable object.");
    } catch (IllegalArgumentException e) {
      // pass
    }
  }

  public void testJsonObject() {
    String json = "{\"aaaaaaaaaaaaaaaaaaaaaaaaa\":"
        + "\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}";
    tc.setCompressionThreshold(8);
    CachedData cd = tc.encode(json);
    assertFalse("Flags shows JSON was compressed",
        (cd.getFlags() & (1L << WhalinTranscoder.COMPRESSED)) != 0);
    assertTrue("JSON was incorrectly encoded",
        Arrays.equals(json.getBytes(), cd.getData()));
    assertEquals("JSON was harmed, should not have been", json, tc.decode(cd));
  }

  public void testCompressedStringNotSmaller() throws Exception {
    String s1 = "This is a test simple string that will not be compressed.";
    // Reduce the compression threshold so it'll attempt to compress it.
    tc.setCompressionThreshold(8);
    CachedData cd = tc.encode(s1);
    // This should *not* be compressed because it is too small
    assertEquals(WhalinTranscoder.SPECIAL_STRING, cd.getFlags());
    assertTrue(Arrays.equals(s1.getBytes(), cd.getData()));
    assertEquals(s1, tc.decode(cd));
  }

  public void testCompressedString() throws Exception {
    // This one will actually compress
    String s1 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    tc.setCompressionThreshold(8);
    CachedData cd = tc.encode(s1);
    assertEquals(WhalinTranscoder.COMPRESSED | WhalinTranscoder.SPECIAL_STRING,
        cd.getFlags());
    assertFalse(Arrays.equals(s1.getBytes(), cd.getData()));
    assertEquals(s1, tc.decode(cd));
  }

  public void testObject() throws Exception {
    Calendar c = Calendar.getInstance();
    CachedData cd = tc.encode(c);
    assertEquals(WhalinTranscoder.SERIALIZED, cd.getFlags());
    assertEquals(c, tc.decode(cd));
  }

  public void testCompressedObject() throws Exception {
    tc.setCompressionThreshold(8);
    Calendar c = Calendar.getInstance();
    CachedData cd = tc.encode(c);
    assertEquals(WhalinTranscoder.SERIALIZED | WhalinTranscoder.COMPRESSED,
        cd.getFlags());
    assertEquals(c, tc.decode(cd));
  }

  public void testUnencodeable() throws Exception {
    try {
      CachedData cd = tc.encode(new Object());
      fail("Should fail to serialize, got" + cd);
    } catch (IllegalArgumentException e) {
      // pass
    }
  }

  public void testUndecodeable() throws Exception {
    CachedData cd = new CachedData(Integer.MAX_VALUE
        & ~(WhalinTranscoder.COMPRESSED | WhalinTranscoder.SERIALIZED),
        tu.encodeInt(Integer.MAX_VALUE), tc.getMaxSize());
    assertNull(tc.decode(cd));
  }

  public void testUndecodeableSerialized() throws Exception {
    CachedData cd = new CachedData(WhalinTranscoder.SERIALIZED,
        tu.encodeInt(Integer.MAX_VALUE), tc.getMaxSize());
    assertNull(tc.decode(cd));
  }

  public void testUndecodeableCompressed() throws Exception {
    CachedData cd = new CachedData(WhalinTranscoder.COMPRESSED,
        tu.encodeInt(Integer.MAX_VALUE), tc.getMaxSize());
    assertNull(tc.decode(cd));
  }

  @Override
  protected int getStringFlags() {
    return WhalinTranscoder.SPECIAL_STRING;
  }
}
