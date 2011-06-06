package net.spy.memcached.ops;


/**
 * Gat operation.
 */
public interface GetAndTouchOperation extends KeyedOperation {

	/**
	 * Operation callback for the gat request.
	 */
	interface Callback extends OperationCallback {
		/**
		 * Callback for each result from a gat.
		 *
		 * @param key the key that was retrieved
		 * @param flags the flags for this value
		 * @param data the data stored under this key
		 */
		void gotData(String key, int flags, long cas, byte[] data);
	}

}
