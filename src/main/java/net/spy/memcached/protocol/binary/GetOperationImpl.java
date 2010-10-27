package net.spy.memcached.protocol.binary;

import java.util.Collection;
import java.util.Collections;

import net.spy.memcached.ops.GetAndTouchOperation;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.GetlOperation;
import net.spy.memcached.ops.GetsOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;

class GetOperationImpl extends OperationImpl
	implements GetOperation, GetsOperation, GetlOperation,
	GetAndTouchOperation {

	static final int GET_CMD=0;
	static final int GETL_CMD=0x94;
	static final int GAT_CMD=0x1d;

	/**
	 * Length of the extra header stuff for a GET response.
	 */
	static final int EXTRA_HDR_LEN=4;

	private final String key;
	private final int exp;
	private final int cmd;

	public GetOperationImpl(String k, GetOperation.Callback cb) {
		super(GET_CMD, generateOpaque(), cb);
		key=k;
		exp=0;
		cmd=GET_CMD;
	}

	public GetOperationImpl(String k, GetsOperation.Callback cb) {
		super(GET_CMD, generateOpaque(), cb);
		key=k;
		exp=0;
		cmd=GET_CMD;
	}

	public GetOperationImpl(String k, int e, GetlOperation.Callback cb) {
		super(GETL_CMD, generateOpaque(), cb);
		key=k;
		exp=e;
		cmd=GETL_CMD;
	}

	public GetOperationImpl(String k, int e, GetAndTouchOperation.Callback cb) {
		super(GAT_CMD, generateOpaque(), cb);
		key=k;
		exp=e;
		cmd=GAT_CMD;
	}

	@Override
	public void initialize() {
		if (cmd == GETL_CMD) {
			prepareBuffer(key, 0, EMPTY_BYTES, 0, exp);
		} else if (cmd == GAT_CMD) {
			prepareBuffer(key, 0, EMPTY_BYTES, exp);
		} else {
			prepareBuffer(key, 0, EMPTY_BYTES);
		}
	}

	@Override
	protected void decodePayload(byte[] pl) {
		final int flags=decodeInt(pl, 0);
		final byte[] data=new byte[pl.length - EXTRA_HDR_LEN];
		System.arraycopy(pl, EXTRA_HDR_LEN, data, 0, pl.length-EXTRA_HDR_LEN);
		// Assume we're processing a get unless the cast fails.
		OperationCallback cb = getCallback();
		if (cb instanceof GetOperation.Callback) {
			GetOperation.Callback gcb=(GetOperation.Callback)cb;
			gcb.gotData(key, flags, data);
		} else if (cb instanceof GetsOperation.Callback) {
			GetsOperation.Callback gcb=(GetsOperation.Callback)cb;
			gcb.gotData(key, flags, responseCas, data);
		} else if (cb instanceof GetlOperation.Callback) {
			//getl return the key and value. So we need to strip off the key
			byte[] value = new byte[data.length-key.length()];
			System.arraycopy(data, key.length(), value, 0, data.length-key.length());
			GetlOperation.Callback gcb=(GetlOperation.Callback)cb;
			gcb.gotData(key, flags, responseCas, value);
		} else if (cb instanceof GetAndTouchOperation.Callback) {
			GetAndTouchOperation.Callback gcb=(GetAndTouchOperation.Callback)cb;
			gcb.gotData(key, flags, responseCas, data);
		} else {
			throw new ClassCastException("Couldn't convert " + cb + "to a relevent op");
		}
		getCallback().receivedStatus(STATUS_OK);
	}

	@Override
	protected OperationStatus getStatusForErrorCode(int errCode, byte[] errPl) {
        OperationStatus baseStatus = super.getStatusForErrorCode(errCode, errPl);
        if (baseStatus != null) {
            return baseStatus;
        }
        return errCode == ERR_NOT_FOUND ? NOT_FOUND_STATUS : null;
	}

	public Collection<String> getKeys() {
		return Collections.singleton(key);
	}

}
