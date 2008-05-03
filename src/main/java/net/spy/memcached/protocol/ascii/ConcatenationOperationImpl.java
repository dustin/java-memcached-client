package net.spy.memcached.protocol.ascii;

import net.spy.memcached.ops.ConcatenationOperation;
import net.spy.memcached.ops.ConcatenationType;
import net.spy.memcached.ops.OperationCallback;

/**
 * Operation for ascii concatenations.
 */
public class ConcatenationOperationImpl extends BaseStoreOperationImpl
		implements ConcatenationOperation {

	public ConcatenationOperationImpl(ConcatenationType t, String k,
		byte[] d, OperationCallback cb) {
		super(t.name(), k, 0, 0, d, cb);
	}

}
