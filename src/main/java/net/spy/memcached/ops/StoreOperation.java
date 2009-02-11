package net.spy.memcached.ops;

/**
 * Operation that represents object storage.
 */
public interface StoreOperation extends KeyedOperation {

	/**
	 * Get the store type used by this operation.
	 */
	StoreType getStoreType();

	/**
	 * Get the flags to be set.
	 */
	int getFlags();

	/**
	 * Get the expiration value to be set.
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
	byte[] getData();
}