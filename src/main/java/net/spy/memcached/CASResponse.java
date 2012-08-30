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

/**
 * Response codes for a CAS operation.
 */
public enum CASResponse {
  /**
   * Status indicating that the CAS was successful and the new value is stored
   * in the cache.
   */
  OK,
  /**
   * Status indicating the value was not found in the cache (an add operation
   * may be issued to store the value).
   */
  NOT_FOUND,
  /**
   * Status indicating the value was found in the cache, but exists with a
   * different CAS value than expected. In this case, the value must be
   * refetched and the CAS operation tried again.
   */
  EXISTS,
  /**
   * Status indicating there was an error in specifying the arguments for
   * the Observe.
   */
  OBSERVE_ERROR_IN_ARGS,
  /**
   * Status indicating the CAS operation succeeded but the value was
   * subsequently modified during Observe.
   */
  OBSERVE_MODIFIED,
  /**
   * Status indicating there was a Timeout in the Observe operation.
   */
  OBSERVE_TIMEOUT;
}
