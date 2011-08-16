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

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of known hashing algorithms for locating a server for a key. Useful
 * when configuring from files using algorithm names.
 *
 * <p>
 * Please, make sure you register your algorithm with {
 * {@link #registerHashAlgorithm(String, HashAlgorithm)} before referring to it
 * by name
 */
public final class HashAlgorithmRegistry {

  private HashAlgorithmRegistry() {
    // Empty
  }

  /**
   * Internal registry storage.
   */
  private static final Map<String, HashAlgorithm> REGISTRY =
      new HashMap<String, HashAlgorithm>();
  // initializing with the default algorithms implementation
  static {
    for (DefaultHashAlgorithm alg : DefaultHashAlgorithm.values()) {
      registerHashAlgorithm(alg.name().replace("_HASH", "").toLowerCase(),
          alg);
    }
  }

  /**
   * Registers provided {@link HashAlgorithm} instance with the given name. Name
   * is not case sensitive. Any registered algorithm with the same name will be
   * substituted
   *
   * @param name name of the algorithm
   * @param alg algorithm instance to register
   */
  public static synchronized void registerHashAlgorithm(String name,
      HashAlgorithm alg) {
    validateName(name);
    validateAlgorithm(alg);
    REGISTRY.put(name.toLowerCase(), alg);
  }

  /**
   * Tries to find selected hash algorithm using name provided.
   *
   * <p>
   * Note, that lookup is being performed using name's lower-case value
   *
   * @param name the algorithm name to be used for lookup
   * @return a {@link HashAlgorithm} instance or <code>null</code> if there's no
   *         algorithm with the specified name
   */
  public static synchronized HashAlgorithm lookupHashAlgorithm(String name) {
    validateName(name);
    return REGISTRY.get(name.toLowerCase());
  }

  /**
   * Validates name of the algorithm. A non-empty name should be provided An
   * {@link IllegalArgumentException} is being thrown in case of incorrect name
   *
   * @param name a name to validate
   */
  private static void validateName(String name) {
    if (name == null || "".equals(name)) {
      throw new IllegalArgumentException("HashAlgorithm name should be"
          + "provided in order to perform the lookup: either NULL or "
          + "empty string has been provided");
    }
  }

  /**
   * Validates algorithm instance. A non-<code>null</code> instance should be
   * provided. An {@link IllegalArgumentException} is being thrown in case of
   * <code>null</code> instance
   *
   * @param alg a {@link HashAlgorithm} instance to validate
   */
  private static void validateAlgorithm(HashAlgorithm alg) {
    if (alg == null) {
      throw new IllegalArgumentException("HashAlgorithm instance should be "
          + "provided in order to register a new algorithm");
    }
  }
}
