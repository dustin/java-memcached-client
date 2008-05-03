package net.spy.memcached.protocol.binary;

import net.spy.memcached.ops.ConcatenationOperation;
import net.spy.memcached.ops.ConcatenationType;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;

class ConcatenationOperationImpl extends OperationImpl
	implements ConcatenationOperation {

	private static final int APPEND=0x0e;
	private static final int PREPEND=0x0f;

	private final String key;
	private final long cas;
	private final byte[] data;

	private static int cmdMap(ConcatenationType t) {
		int rv=-1;
		switch(t) {
			case append: rv=APPEND; break;
			case prepend: rv=PREPEND; break;
		}
		// Check fall-through.
		assert rv != -1 : "Unhandled store type:  " + t;
		return rv;
	}

	public ConcatenationOperationImpl(ConcatenationType t, String k,
			byte[] d, long c, OperationCallback cb) {
		super(cmdMap(t), generateOpaque(), cb);
		key=k;
		data=d;
		cas=c;
	}

	@Override
	public void initialize() {
		prepareBuffer(key, cas, data);
	}

	@Override
	protected OperationStatus getStatusForErrorCode(int errCode, byte[] errPl) {
		OperationStatus rv=null;
		switch(errCode) {
			case ERR_EXISTS:
				rv=EXISTS_STATUS;
				break;
			case ERR_NOT_FOUND:
				rv=NOT_FOUND_STATUS;
				break;
			case ERR_NOT_STORED:
				rv=NOT_FOUND_STATUS;
				break;
		}
		return rv;
	}

}
