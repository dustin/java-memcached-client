package net.spy.memcached.protocol.binary;

import net.spy.memcached.ops.NoopOperation;
import net.spy.memcached.ops.OperationCallback;

/**
 * Implementation of a noop operation.
 */
public class NoopOperationImpl extends OperationImpl implements NoopOperation {

	private static final int CMD=10;

	public NoopOperationImpl(OperationCallback cb) {
		super(CMD, generateOpaque(), cb);
	}

	@Override
	public void initialize() {
		prepareBuffer("", EMPTY_BYTES);
	}

}
