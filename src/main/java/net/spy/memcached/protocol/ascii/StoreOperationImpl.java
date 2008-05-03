// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.protocol.ascii;


import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.StoreType;

/**
 * Operation to store data in a memcached server.
 */
final class StoreOperationImpl extends BaseStoreOperationImpl
	implements StoreOperation {

	public StoreOperationImpl(StoreType t, String k, int f, int e,
			byte[] d, OperationCallback cb) {
		super(t.name(), k, f, e, d, cb);
	}

}
