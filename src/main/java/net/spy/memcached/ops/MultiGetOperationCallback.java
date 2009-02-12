package net.spy.memcached.ops;

/**
 * MultiOperationCallback for get operations.
 */
public class MultiGetOperationCallback extends MultiOperationCallback
	implements GetOperation.Callback {

	public MultiGetOperationCallback(OperationCallback original, int todo) {
		super(original, todo);
	}

	public void gotData(String key, int flags, byte[] data) {
		((GetOperation.Callback)originalCallback).gotData(key, flags, data);
	}

}
