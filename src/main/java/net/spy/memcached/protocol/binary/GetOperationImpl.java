package net.spy.memcached.protocol.binary;

import java.util.Collection;
import java.util.Collections;

import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.GetsOperation;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.util.KeyUtil;

class GetOperationImpl extends OperationImpl
	implements GetOperation, GetsOperation {

	static final int CMD=0;

	/**
	 * Length of the extra header stuff for a GET response.
	 */
	static final int EXTRA_HDR_LEN=4;

	private final byte[] key;

	public GetOperationImpl(byte[] k, GetOperation.Callback cb) {
		super(CMD, generateOpaque(), cb);
		key=k;
	}

	public GetOperationImpl(byte[] k, GetsOperation.Callback cb) {
		super(CMD, generateOpaque(), cb);
		key=k;
	}

	@Override
	public void initialize() {
		prepareBuffer(key, 0, EMPTY_BYTES);
	}

	@Override
	protected void decodePayload(byte[] pl) {
		final int flags=decodeInt(pl, 0);
		final byte[] data=new byte[pl.length - EXTRA_HDR_LEN];
		System.arraycopy(pl, EXTRA_HDR_LEN, data, 0, pl.length-EXTRA_HDR_LEN);
		// Assume we're processing a get unless the cast fails.
		try {
			GetOperation.Callback cb=(GetOperation.Callback)getCallback();
			cb.gotData(KeyUtil.getKeyString(key), flags, data);
		} catch(ClassCastException e) {
			GetsOperation.Callback cb=(GetsOperation.Callback)getCallback();
			cb.gotData(KeyUtil.getKeyString(key), flags, responseCas, data);
		}
		getCallback().receivedStatus(STATUS_OK);
	}

	@Override
	protected OperationStatus getStatusForErrorCode(int errCode, byte[] errPl) {
		return errCode == ERR_NOT_FOUND ? NOT_FOUND_STATUS : null;
	}

	public Collection<String> getKeys() {
		return Collections.singleton(KeyUtil.getKeyString(key));
	}

}
