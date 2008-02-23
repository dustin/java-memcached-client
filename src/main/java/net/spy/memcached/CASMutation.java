package net.spy.memcached;

/**
 * Defines a mutation mechanism for a high-level CAS client interface.
 */
public interface CASMutation<T> {

	/**
	 * Get the new value to replace the current value.
	 *
	 * @param current the current value in the cache
	 * @return the replacement value
	 */
	T getNewValue(T current);
}
