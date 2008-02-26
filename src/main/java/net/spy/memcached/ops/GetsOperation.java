package net.spy.memcached.ops;

import java.util.Collection;

/**
 * Gets operation (get with CAS identifier support).
 */
public interface GetsOperation extends Operation {

	/**
	 * Operation callback for the Gets request.
	 */
	public interface Callback extends OperationCallback {
		/**
		 * Callback for each result from a gets.
		 *
		 * @param key the key that was retrieved
		 * @param flags the flags for this value
		 * @param cas the CAS value for this record
		 * @param data the data stored under this key
		 */
		void gotData(String key, int flags, long cas, byte[] data);
	}

	/**
	 * Get the keys requested in this GetsOperation.
	 */
	Collection<String> getKeys();

}