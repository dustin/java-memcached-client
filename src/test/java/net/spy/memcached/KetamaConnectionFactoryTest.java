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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * A very basic test that the KetamaConnectionFactory returns both the correct
 * hash algorithm and the correct node locator.
 */
public class KetamaConnectionFactoryTest extends TestCase {

  /*
   * This *is* kinda lame, but it tests the specific differences from the
   * DefaultConnectionFactory.
   */
  public void testCorrectTypes() {
    ConnectionFactory factory = new KetamaConnectionFactory();

    NodeLocator locator = factory.createLocator(new ArrayList<MemcachedNode>());
    assertTrue(locator instanceof KetamaNodeLocator);
  }

  /*
   * Test initialization with defaults
   */
  public void testDefaultProperties() {
      KetamaConnectionFactory connectionFactory = new KetamaConnectionFactory();
      assertEquals(connectionFactory.getHashAlg(), DefaultHashAlgorithm.KETAMA_HASH);
      assertTrue(connectionFactory.getWeights().isEmpty());
      assertEquals(connectionFactory.getKetamaNodeKeyFormat(), KetamaNodeKeyFormatter.Format.SPYMEMCACHED);
  }

  /*
   * Test overwriting the weights, hash and node key format
   */
  public void testSettingProperties() {
        Map<InetSocketAddress, Integer> weights = new HashMap<InetSocketAddress, Integer>();
        weights.put(new InetSocketAddress("localhost", 11211), 8);
        KetamaConnectionFactory connectionFactory = new KetamaConnectionFactory(
                1, 1, 1, DefaultHashAlgorithm.FNV1_32_HASH,
                KetamaNodeKeyFormatter.Format.LIBMEMCACHED, weights);
        assertEquals(connectionFactory.getWeights(), weights);
        assertEquals(connectionFactory.getHashAlg(), DefaultHashAlgorithm.FNV1_32_HASH);
        assertEquals(connectionFactory.getKetamaNodeKeyFormat(), KetamaNodeKeyFormatter.Format.LIBMEMCACHED);
  }
}
