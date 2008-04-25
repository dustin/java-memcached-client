package net.spy.memcached.protocol.binary;

import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;

class DeleteOperationImpl extends OperationImpl implements
		DeleteOperation {

	private static final int CMD=4;

	private final String key;
	private final int when;
	private final long cas;

	public DeleteOperationImpl(String k, int w, OperationCallback cb) {
		this(k, w, 0, cb);
	}

	public DeleteOperationImpl(String k, int w, long c, OperationCallback cb) {
		super(CMD, generateOpaque(), cb);
		key=k;
		when=w;
		cas=c;
	}

	@Override
	public void initialize() {
		prepareBuffer(key, cas, EMPTY_BYTES, when);
	}

	@Override
	protected OperationStatus getStatusForErrorCode(int errCode, byte[] errPl) {
		return errCode == ERR_NOT_FOUND ? NOT_FOUND_STATUS : null;
	}

}
