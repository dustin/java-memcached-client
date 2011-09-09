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

package net.spy.memcached.transcoders;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.spy.memcached.CachedData;
import net.spy.memcached.compat.CloseUtil;
import net.spy.memcached.compat.SpyObject;

/**
 * Base class for any transcoders that may want to work with serialized or
 * compressed data.
 */
public abstract class BaseSerializingTranscoder extends SpyObject {

  /**
   * Default compression threshold value.
   */
  public static final int DEFAULT_COMPRESSION_THRESHOLD = 16384;

  private static final String DEFAULT_CHARSET = "UTF-8";

  protected int compressionThreshold = DEFAULT_COMPRESSION_THRESHOLD;
  protected String charset = DEFAULT_CHARSET;

  private final int maxSize;

  /**
   * Initialize a serializing transcoder with the given maximum data size.
   */
  public BaseSerializingTranscoder(int max) {
    super();
    maxSize = max;
  }

  public boolean asyncDecode(CachedData d) {
    return false;
  }

  /**
   * Set the compression threshold to the given number of bytes. This transcoder
   * will attempt to compress any data being stored that's larger than this.
   *
   * @param to the number of bytes
   */
  public void setCompressionThreshold(int to) {
    compressionThreshold = to;
  }

  /**
   * Set the character set for string value transcoding (defaults to UTF-8).
   */
  public void setCharset(String to) {
    // Validate the character set.
    try {
      new String(new byte[97], to);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    charset = to;
  }

  /**
   * Get the bytes representing the given serialized object.
   */
  protected byte[] serialize(Object o) {
    if (o == null) {
      throw new NullPointerException("Can't serialize null");
    }
    byte[] rv=null;
    ByteArrayOutputStream bos = null;
    ObjectOutputStream os = null;
    try {
      bos = new ByteArrayOutputStream();
      os = new ObjectOutputStream(bos);
      os.writeObject(o);
      os.close();
      bos.close();
      rv = bos.toByteArray();
    } catch (IOException e) {
      throw new IllegalArgumentException("Non-serializable object", e);
    } finally {
      CloseUtil.close(os);
      CloseUtil.close(bos);
    }
    return rv;
  }

  /**
   * Get the object represented by the given serialized bytes.
   */
  protected Object deserialize(byte[] in) {
    Object rv=null;
    ByteArrayInputStream bis = null;
    ObjectInputStream is = null;
    try {
      if(in != null) {
        bis=new ByteArrayInputStream(in);
        is=new ObjectInputStream(bis);
        rv=is.readObject();
        is.close();
        bis.close();
      }
    } catch (IOException e) {
      getLogger().warn("Caught IOException decoding %d bytes of data",
          in == null ? 0 : in.length, e);
    } catch (ClassNotFoundException e) {
      getLogger().warn("Caught CNFE decoding %d bytes of data",
          in == null ? 0 : in.length, e);
    } finally {
      CloseUtil.close(is);
      CloseUtil.close(bis);
    }
    return rv;
  }

  /**
   * Compress the given array of bytes.
   */
  protected byte[] compress(byte[] in) {
    if (in == null) {
      throw new NullPointerException("Can't compress null");
    }
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    GZIPOutputStream gz = null;
    try {
      gz = new GZIPOutputStream(bos);
      gz.write(in);
    } catch (IOException e) {
      throw new RuntimeException("IO exception compressing data", e);
    } finally {
      CloseUtil.close(gz);
      CloseUtil.close(bos);
    }
    byte[] rv = bos.toByteArray();
    getLogger().debug("Compressed %d bytes to %d", in.length, rv.length);
    return rv;
  }

  /**
   * Decompress the given array of bytes.
   *
   * @return null if the bytes cannot be decompressed
   */
  protected byte[] decompress(byte[] in) {
    ByteArrayOutputStream bos = null;
    if(in != null) {
      ByteArrayInputStream bis = new ByteArrayInputStream(in);
      bos = new ByteArrayOutputStream();
      GZIPInputStream gis = null;
      try {
        gis = new GZIPInputStream(bis);

        byte[] buf = new byte[8192];
        int r = -1;
        while ((r = gis.read(buf)) > 0) {
          bos.write(buf, 0, r);
        }
      } catch (IOException e) {
        getLogger().warn("Failed to decompress data", e);
        bos = null;
      } finally {
        CloseUtil.close(gis);
        CloseUtil.close(bis);
        CloseUtil.close(bos);
      }
    }
    return bos == null ? null : bos.toByteArray();
  }

  /**
   * Decode the string with the current character set.
   */
  protected String decodeString(byte[] data) {
    String rv = null;
    try {
      if (data != null) {
        rv = new String(data, charset);
      }
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    return rv;
  }

  /**
   * Encode a string into the current character set.
   */
  protected byte[] encodeString(String in) {
    byte[] rv = null;
    try {
      rv = in.getBytes(charset);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    return rv;
  }

  public int getMaxSize() {
    return maxSize;
  }
}
