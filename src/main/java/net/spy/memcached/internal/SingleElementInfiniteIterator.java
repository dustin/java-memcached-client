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

package net.spy.memcached.internal;

import java.util.Iterator;

/**
 * An iterator that returns a single element for as many elements as are needed
 * from the iterator; in other words, #hasNext() never returns false.
 */
public class SingleElementInfiniteIterator<T> implements Iterator<T> {
  private final T element;

  /**
   * Construct a iterator tat returns the input element an infinite number of
   * times.
   *
   * @param element the element that #next() should return
   */
  public SingleElementInfiniteIterator(T element) {
    this.element = element;
  }

  public boolean hasNext() {
    return true;
  }

  public T next() {
    return element;
  }

  public void remove() {
    throw new UnsupportedOperationException("Cannot remove from this "
        + "iterator.");
  }
}
