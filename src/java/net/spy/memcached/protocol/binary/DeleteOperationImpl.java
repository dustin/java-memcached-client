package net.spy.memcached.protocol.binary;

import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.OperationCallback;

class DeleteOperationImpl extends OperationImpl implements
		DeleteOperation {

	private static final int CMD=4;

	private final String key;

	public DeleteOperationImpl(String k, OperationCallback cb) {
		super(CMD, generateOpaque(), cb);
		key=k;
	}

	@Override
	public void initialize() {
		prepareBuffer(key, EMPTY_BYTES);
	}

}
