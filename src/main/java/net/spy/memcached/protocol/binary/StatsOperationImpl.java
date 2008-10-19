package net.spy.memcached.protocol.binary;

import java.io.IOException;

import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.StatsOperation;

public class StatsOperationImpl extends OperationImpl
	implements StatsOperation {

	private static final int CMD = 0x10;
	private final String key;

	public StatsOperationImpl(String arg, StatsOperation.Callback c) {
		super(CMD, generateOpaque(), c);
		key=(arg == null) ? "" : arg;
	}

	@Override
	public void initialize() {
		prepareBuffer(key, 0, EMPTY_BYTES);
	}

	@Override
	protected void finishedPayload(byte[] pl) throws IOException {
		if(keyLen > 0) {
			final byte[] keyBytes=new byte[keyLen];
			final byte[] data=new byte[pl.length - keyLen];
			System.arraycopy(pl, 0, keyBytes, 0, keyLen);
			System.arraycopy(pl, keyLen, data, 0, pl.length-keyLen);
			Callback cb=(Callback)getCallback();
			cb.gotStat(new String(keyBytes, "UTF-8"),
					new String(data, "UTF-8"));
		} else {
			getCallback().receivedStatus(STATUS_OK);
			transitionState(OperationState.COMPLETE);
		}
		resetInput();
	}

}
