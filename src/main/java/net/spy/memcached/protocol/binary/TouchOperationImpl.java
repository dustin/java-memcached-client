package net.spy.memcached.protocol.binary;

import net.spy.memcached.ops.OperationCallback;

/**
 * Operation to reset a timeout in Membase server.
 */
public class TouchOperationImpl extends SingleKeyOperationImpl {

	static final int CMD=0x1c;

	private final int exp;

	protected TouchOperationImpl(String k, int e, OperationCallback cb) {
		super(CMD, generateOpaque(), k, cb);
		exp=e;
	}

	@Override
	public void initialize() {
		prepareBuffer(key, 0, EMPTY_BYTES, exp);
	}
}
