/**
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

/**
 * Known hashing algorithms for locating a server for a key. Note that all hash
 * algorithms return 64-bits of hash, but only the lower 32-bits are
 * significant. This allows a positive 32-bit number to be returned for all
 * cases.
 */
public enum DefaultHashAlgorithm implements HashAlgorithm {

  /**
   * Native hash (String.hashCode()).
   */
  NATIVE_HASH,
  /**
   * CRC_HASH as used by the perl API. This will be more consistent both
   * across multiple API users as well as java versions, but is mostly likely
   * significantly slower.
   */
  CRC_HASH,
  /**
   * FNV hashes are designed to be fast while maintaining a low collision rate.
   * The FNV speed allows one to quickly hash lots of data while maintaining a
   * reasonable collision rate.
   *
   * @see <a href="http://www.isthe.com/chongo/tech/comp/fnv/">fnv
   *      comparisons</a>
   * @see <a href="http://en.wikipedia.org/wiki/Fowler_Noll_Vo_hash">fnv at
   *      wikipedia</a>
   */
  FNV1_64_HASH,
  /**
   * Variation of FNV.
   */
  FNV1A_64_HASH,
  /**
   * 32-bit FNV1.
   */
  FNV1_32_HASH,
  /**
   * 32-bit FNV1a.
   */
  FNV1A_32_HASH,
  /**
   * MD5-based hash algorithm used by ketama.
   */
  KETAMA_HASH;

  private static final long FNV_64_INIT = 0xcbf29ce484222325L;
  private static final long FNV_64_PRIME = 0x100000001b3L;

  private static final long FNV_32_INIT = 2166136261L;
  private static final long FNV_32_PRIME = 16777619;

  private static MessageDigest md5Digest = null;

  static {
    try {
      md5Digest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("MD5 not supported", e);
    }
  }

  /**
   * Compute the hash for the given key.
   *
   * @return a positive integer hash
   */
  public long hash(final String k) {
    long rv = 0;
    int len = k.length();
    switch (this) {
    case NATIVE_HASH:
      rv = k.hashCode();
      break;
    case CRC_HASH:
      // return (crc32(shift) >> 16) & 0x7fff;
      CRC32 crc32 = new CRC32();
      crc32.update(KeyUtil.getKeyBytes(k));
      rv = (crc32.getValue() >> 16) & 0x7fff;
      break;
    case FNV1_64_HASH:
      // Thanks to pierre@demartines.com for the pointer
      rv = FNV_64_INIT;
      for (int i = 0; i < len; i++) {
        rv *= FNV_64_PRIME;
        rv ^= k.charAt(i);
      }
      break;
    case FNV1A_64_HASH:
      rv = FNV_64_INIT;
      for (int i = 0; i < len; i++) {
        rv ^= k.charAt(i);
        rv *= FNV_64_PRIME;
      }
      break;
    case FNV1_32_HASH:
      rv = FNV_32_INIT;
      for (int i = 0; i < len; i++) {
        rv *= FNV_32_PRIME;
        rv ^= k.charAt(i);
      }
      break;
    case FNV1A_32_HASH:
      rv = FNV_32_INIT;
      for (int i = 0; i < len; i++) {
        rv ^= k.charAt(i);
        rv *= FNV_32_PRIME;
      }
      break;
    case KETAMA_HASH:
      byte[] bKey = computeMd5(k);
      rv = ((long) (bKey[3] & 0xFF) << 24)
          | ((long) (bKey[2] & 0xFF) << 16)
          | ((long) (bKey[1] & 0xFF) << 8)
          | (bKey[0] & 0xFF);
      break;
    default:
      assert false;
    }
    return rv & 0xffffffffL; /* Truncate to 32-bits */
  }

  /**
   * Get the md5 of the given key.
   */
  public static byte[] computeMd5(String k) {
    MessageDigest md5;
    try {
      md5 = (MessageDigest) md5Digest.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException("clone of MD5 not supported", e);
    }
    md5.update(KeyUtil.getKeyBytes(k));
    return md5.digest();
  }
}
