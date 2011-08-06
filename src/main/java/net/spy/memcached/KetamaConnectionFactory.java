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

import java.util.List;

/**
 * ConnectionFactory instance that sets up a ketama compatible connection.
 *
 * <p>
 * This implementation piggy-backs on the functionality of the
 * <code>DefaultConnectionFactory</code> in terms of connections and queue
 * handling. Where it differs is that it uses both the <code>
 * KetamaNodeLocator</code> and the <code>HashAlgorithm.KETAMA_HASH</code> to
 * provide consistent node hashing.
 * </p>
 *
 * @see <a href="http://www.last.fm/user/RJ/journal/2007/04/10/392555/">RJ's
 *      blog post</a>
 */
public class KetamaConnectionFactory extends DefaultConnectionFactory {

  /**
   * Create a KetamaConnectionFactory with the given maximum operation
   * queue length, and the given read buffer size.
   *
   * @param opQueueMaxBlockTime the maximum time to block waiting for op
   *        queue operations to complete, in milliseconds
   */
  public KetamaConnectionFactory(int qLen, int bufSize,
      long opQueueMaxBlockTime) {
    super(qLen, bufSize, DefaultHashAlgorithm.KETAMA_HASH);
  }

  /**
   * Create a KetamaConnectionFactory with the default parameters.
   */
  public KetamaConnectionFactory() {
    this(DEFAULT_OP_QUEUE_LEN, DEFAULT_READ_BUFFER_SIZE,
        DEFAULT_OP_QUEUE_MAX_BLOCK_TIME);
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#createLocator(java.util.List)
   */
  @Override
  public NodeLocator createLocator(List<MemcachedNode> nodes) {
    return new KetamaNodeLocator(nodes, getHashAlg());
  }
}
