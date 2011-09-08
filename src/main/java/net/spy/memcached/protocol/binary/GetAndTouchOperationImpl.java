package net.spy.memcached.protocol.binary;

import net.spy.memcached.ops.GetAndTouchOperation;

public class GetAndTouchOperationImpl extends SingleKeyOperationImpl
		implements GetAndTouchOperation {

	static final byte GAT_CMD=0x1d;

	/**
	 * Length of the extra header stuff for a GET response.
	 */
	static final int EXTRA_HDR_LEN=4;

	private final int exp;

	public GetAndTouchOperationImpl(String k, int e, GetAndTouchOperation.Callback cb) {
		super(GAT_CMD, generateOpaque(), k, cb);
		exp=e;
	}

	@Override
	public void initialize() {
		prepareBuffer(key, 0, EMPTY_BYTES, exp);
	}

	@Override
	protected void decodePayload(byte[] pl) {
		final int flags=decodeInt(pl, 0);
		final byte[] data=new byte[pl.length - EXTRA_HDR_LEN];
		System.arraycopy(pl, EXTRA_HDR_LEN, data, 0, pl.length-EXTRA_HDR_LEN);
		GetAndTouchOperation.Callback gcb=(GetAndTouchOperation.Callback)getCallback();
		gcb.gotData(key, flags, responseCas, data);
		getCallback().receivedStatus(STATUS_OK);
	}

	@Override
	public String toString() {
		return super.toString() + " Exp: " + exp;
	}
}
