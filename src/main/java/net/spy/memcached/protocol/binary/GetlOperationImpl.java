package net.spy.memcached.protocol.binary;

import net.spy.memcached.ops.GetlOperation;

public class GetlOperationImpl extends SingleKeyOperationImpl
		implements GetlOperation {

	static final int GETL_CMD=0x94;

	/**
	 * Length of the extra header stuff for a GET response.
	 */
	static final int EXTRA_HDR_LEN=4;

	private final int exp;

	public GetlOperationImpl(String k, int e, GetlOperation.Callback cb) {
		super(GETL_CMD, generateOpaque(), k, cb);
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
		GetlOperation.Callback gcb=(GetlOperation.Callback)getCallback();
		gcb.gotData(key, flags, responseCas, data);
		getCallback().receivedStatus(STATUS_OK);
	}

	@Override
	public String toString() {
		return super.toString() + " Exp: " + exp;
	}
}
