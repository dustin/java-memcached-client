package net.spy.memcached.ops;


/**
 * Gets operation (get with CAS identifier support).
 */
public interface GetsOperation extends KeyedOperation {

	/**
	 * Operation callback for the Gets request.
	 */
	interface Callback extends OperationCallback {
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

}