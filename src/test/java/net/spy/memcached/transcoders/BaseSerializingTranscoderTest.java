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

import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;
import net.spy.memcached.CachedData;

/**
 * Base tests of the base serializing transcoder stuff.
 */
public class BaseSerializingTranscoderTest extends TestCase {

  private Exposer ex;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ex = new Exposer();
  }

  public void testValidCharacterSet() {
    ex.setCharset("KOI8");
  }

  public void testInvalidCharacterSet() {
    try {
      ex.setCharset("Dustin's Kick Ass Character Set");
    } catch (RuntimeException e) {
      assertTrue(e.getCause() instanceof UnsupportedEncodingException);
    }
  }

  public void testCompressNull() {
    try {
      ex.compress(null);
      fail("Expected an assertion error");
    } catch (NullPointerException e) {
      // pass
    }
  }

  public void testDecodeStringNull() {
    assertNull(ex.decodeString(null));
  }

  public void testDeserializeNull() {
    assertNull(ex.deserialize(null));
  }

  public void testEncodeStringNull() {
    try {
      ex.encodeString(null);
      fail("Expected an assertion error");
    } catch (NullPointerException e) {
      // pass
    }
  }

  public void testSerializeNull() {
    try {
      ex.serialize(null);
      fail("Expected an assertion error");
    } catch (NullPointerException e) {
      // pass
    }
  }

  public void testDecompressNull() {
    assertNull(ex.decompress(null));
  }

  public void testUndeserializable() throws Exception {
    byte[] data = { -84, -19, 0, 5, 115, 114, 0, 4, 84, 101, 115, 116, 2, 61,
      102, -87, -28, 17, 52, 30, 2, 0, 1, 73, 0, 9, 115, 111, 109, 101, 116,
      104, 105, 110, 103, 120, 112, 0, 0, 0, 5 };
    assertNull(ex.deserialize(data));
  }

  public void testDeserializable() throws Exception {
    byte[] data = { -84, -19, 0, 5, 116, 0, 5, 104, 101, 108, 108, 111 };
    assertEquals("hello", ex.deserialize(data));
  }

  public void testBadCharsetDecode() {
    ex.overrideCharsetSet("Some Crap");
    try {
      ex.encodeString("Woo!");
      fail("Expected runtime exception");
    } catch (RuntimeException e) {
      assertSame(UnsupportedEncodingException.class, e.getCause().getClass());
    }
  }

  public void testBadCharsetEncode() {
    ex.overrideCharsetSet("Some Crap");
    try {
      ex.decodeString("Woo!".getBytes());
      fail("Expected runtime exception");
    } catch (RuntimeException e) {
      assertSame(UnsupportedEncodingException.class, e.getCause().getClass());
    }
  }

  // Expose the protected methods so I can test them.
  static class Exposer extends BaseSerializingTranscoder {

    public Exposer() {
      super(CachedData.MAX_SIZE);
    }

    public void overrideCharsetSet(String to) {
      charset = to;
    }

    @Override
    public byte[] compress(byte[] in) {
      return super.compress(in);
    }

    @Override
    public String decodeString(byte[] data) {
      return super.decodeString(data);
    }

    @Override
    public byte[] decompress(byte[] in) {
      return super.decompress(in);
    }

    @Override
    public Object deserialize(byte[] in) {
      return super.deserialize(in);
    }

    @Override
    public byte[] encodeString(String in) {
      return super.encodeString(in);
    }

    @Override
    public byte[] serialize(Object o) {
      return super.serialize(o);
    }
  }
}
