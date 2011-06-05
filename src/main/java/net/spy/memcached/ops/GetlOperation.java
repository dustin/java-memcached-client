package net.spy.memcached.ops;


/**
 * Getl operation.
 */
public interface GetlOperation extends KeyedOperation {

	/**
	 * Operation callback for the getl request.
	 */
	interface Callback extends OperationCallback {
		/**
		 * Callback for each result from a getl.
		 *
		 * @param key the key that was retrieved
		 * @param flags the flags for this value
		 * @param data the data stored under this key
		 */
		void gotData(String key, int flags, long cas, byte[] data);
	}

}
