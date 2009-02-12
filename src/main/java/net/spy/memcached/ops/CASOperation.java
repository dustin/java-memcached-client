package net.spy.memcached.ops;

/**
 * Operation that represents compare-and-swap.
 */
public interface CASOperation extends KeyedOperation {

	/**
	 * Get the type of storage used by this CASOperation.
	 */
	StoreType getStoreType();

	/**
	 * Get the CAS value advised for this operation.
	 */
	long getCasValue();

	/**
	 * Get the flags to be set for this operation.
	 */
	int getFlags();

	/**
	 * Get the expiration to be set for this operation.
	 */
	int getExpiration();

	/**
	 * Get the bytes to be set during this operation.
	 *
	 * <p>
	 *   Note, this returns an exact reference to the bytes and the data
	 *   <em>must not</em> be modified.
	 * </p>
	 */
	byte[] getBytes();

}
