package net.spy.memcached.protocol.binary;

import java.util.Collection;
import java.util.Collections;

import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;

class DeleteOperationImpl extends OperationImpl implements
		DeleteOperation {

	private static final int CMD=4;

	private final String key;
	private final long cas;

	public DeleteOperationImpl(String k, OperationCallback cb) {
		this(k, 0, cb);
	}

	public DeleteOperationImpl(String k, long c, OperationCallback cb) {
		super(CMD, generateOpaque(), cb);
		key=k;
		cas=c;
	}

	@Override
	public void initialize() {
		prepareBuffer(key, cas, EMPTY_BYTES);
	}

	@Override
	protected OperationStatus getStatusForErrorCode(int errCode, byte[] errPl) {
		return errCode == ERR_NOT_FOUND ? NOT_FOUND_STATUS : null;
	}

	public Collection<String> getKeys() {
		return Collections.singleton(key);
	}

}
