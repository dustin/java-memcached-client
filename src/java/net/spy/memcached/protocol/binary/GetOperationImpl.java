package net.spy.memcached.protocol.binary;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.OperationErrorType;
import net.spy.memcached.ops.OperationState;

public class GetOperationImpl extends OperationImpl implements GetOperation {

	private static final int CMD=0;

	private final String key;

	public GetOperationImpl(String k, Callback callback) {
		super(CMD, generateOpaque(), callback);
		key=k;
	}

	@Override
	public void initialize() {
		prepareBuffer(key, EMPTY_BYTES);
	}

	@Override
	protected void decodePayload(byte[] pl) {
		final int flags=decodeInt(pl, 0);
		final byte[] data=new byte[pl.length - 4];
		System.arraycopy(pl, 4, data, 0, pl.length-4);
		Callback cb=(Callback)getCallback();
		cb.gotData(key, flags, data);
		cb.receivedStatus(STATUS_OK);
	}

	@Override
	protected void handleError(int errCode, byte[] errPl) throws IOException {
		if(errCode == NOT_FOUND) {
			getCallback().receivedStatus(STATUS_OK);
			transitionState(OperationState.COMPLETE);
		} else {
			handleError(OperationErrorType.SERVER, new String(errPl));
		}
	}

	public Collection<String> getKeys() {
		return Collections.singleton(key);
	}

}
