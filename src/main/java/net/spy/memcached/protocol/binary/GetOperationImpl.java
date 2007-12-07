package net.spy.memcached.protocol.binary;

import java.util.Collection;
import java.util.Collections;

import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.OperationStatus;

public class GetOperationImpl extends OperationImpl implements GetOperation {

	static final int CMD=0;

	private final String key;

	public GetOperationImpl(String k, Callback cb) {
		super(CMD, generateOpaque(), cb);
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
	protected OperationStatus getStatusForErrorCode(int errCode, byte[] errPl) {
		return errCode == ERR_NOT_FOUND ? NOT_FOUND_STATUS : null;
	}

	public Collection<String> getKeys() {
		return Collections.singleton(key);
	}

}
