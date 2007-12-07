package net.spy.memcached.ops;

import java.util.Collection;

/**
 * Get operation.
 */
public interface GetOperation extends Operation {

	/**
	 * Operation callback for the get request.
	 */
	public interface Callback extends OperationCallback {
		/**
		 * Callback for each result from a get.
		 *
		 * @param key the key that was retrieved
		 * @param flags the flags for this value
		 * @param data the data stored under this key
		 */
		void gotData(String key, int flags, byte[] data);
	}

	/**
	 * Get the keys requested in this GetOperation.
	 */
	Collection<String> getKeys();

}