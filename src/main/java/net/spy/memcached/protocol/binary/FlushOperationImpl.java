package net.spy.memcached.protocol.binary;

import net.spy.memcached.ops.FlushOperation;
import net.spy.memcached.ops.OperationCallback;

class FlushOperationImpl extends OperationImpl implements FlushOperation {

	private static final int CMD=7;

	public FlushOperationImpl(OperationCallback cb) {
		super(CMD, generateOpaque(), cb);
	}

	@Override
	public void initialize() {
		prepareBuffer("", EMPTY_BYTES);
	}

}