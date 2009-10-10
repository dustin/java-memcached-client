package net.spy.memcached.protocol.binary;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.SASLMechsOperation;

class SASLMechsOperationImpl extends OperationImpl implements
		SASLMechsOperation {

	private static final int CMD = 0x20;

	public SASLMechsOperationImpl(OperationCallback cb) {
		super(CMD, generateOpaque(), cb);
	}

	@Override
	public void initialize() {
		prepareBuffer("", 0, EMPTY_BYTES);
	}

	@Override
	protected void decodePayload(byte[] pl) {
		getCallback().receivedStatus(
				new OperationStatus(true, new String(pl)));
	}

}
