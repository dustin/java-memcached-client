package net.spy.memcached.internal;

import java.lang.UnsupportedOperationException;
import java.util.Iterator;

/**
 * An iterator that returns a single element for as many elements as
 * are needed from the iterator; in other words, #hasNext() never
 * returns false.
 */
public class SingleElementInfiniteIterator<T>
	implements Iterator<T> {
	private final T element;

	/**
	 * Construct a iterator tat returns the input element an
	 * infinite number of times.
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
		throw new UnsupportedOperationException("Cannot remove from this iterator.");
	}
}
