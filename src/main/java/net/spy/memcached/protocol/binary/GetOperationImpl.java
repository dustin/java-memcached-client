package net.spy.memcached.protocol.binary;

import net.spy.memcached.ops.GetOperation;

class GetOperationImpl extends SingleKeyOperationImpl
	implements GetOperation {

	static final byte GET_CMD=0x00;

	/**
	 * Length of the extra header stuff for a GET response.
	 */
	static final int EXTRA_HDR_LEN=4;

	public GetOperationImpl(String k, GetOperation.Callback cb) {
		super(GET_CMD, generateOpaque(), k, cb);
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
		GetOperation.Callback gcb=(GetOperation.Callback)getCallback();
		gcb.gotData(key, flags, data);
		getCallback().receivedStatus(STATUS_OK);
	}

}
