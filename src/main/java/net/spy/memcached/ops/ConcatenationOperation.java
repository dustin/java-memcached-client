package net.spy.memcached.ops;

/**
 * ConcatenationOperation is used to append or prepend data to an existing
 * object in the cache.
 */
public interface ConcatenationOperation extends KeyedOperation {

	/**
	 * Get the concatenation type for this operation.
	 */
	ConcatenationType getStoreType();

	/**
	 * Get the CAS value sent with this operation.
	 */
	long getCasValue();

	/**
	 * Get the bytes to be set during this operation.
	 *
	 * <p>
	 *   Note, this returns an exact reference to the bytes and the data
	 *   <em>must not</em> be modified.
	 * </p>
	 */
	byte[] getData();
}
