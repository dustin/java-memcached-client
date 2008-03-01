package net.spy.memcached.protocol.binary;

import net.spy.memcached.ops.CASOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.StoreType;

class StoreOperationImpl extends OperationImpl
	implements StoreOperation, CASOperation {

	private static int SET=1;
	private static int ADD=2;
	private static int REPLACE=3;

	private final String key;
	private final int flags;
	private final int exp;
	private final long cas;
	private final byte[] data;

	private static int cmdMap(StoreType t) {
		int rv=-1;
		switch(t) {
			case set: rv=SET; break;
			case add: rv=ADD; break;
			case replace: rv=REPLACE; break;
		}
		// Check fall-through.
		assert rv != -1 : "Unhandled store type:  " + t;
		return rv;
	}

	public StoreOperationImpl(StoreType t, String k, int f, int e,
			byte[] d, OperationCallback cb) {
		this(t, k, f, e, d, 0, cb);
	}

	public StoreOperationImpl(StoreType t, String k, int f, int e,
			byte[] d, long c, OperationCallback cb) {
		super(cmdMap(t), generateOpaque(), cb);
		key=k;
		flags=f;
		exp=e;
		data=d;
		cas=c;
	}

	@Override
	public void initialize() {
		prepareBuffer(key, data, flags, exp, cas);
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
		}
		return rv;
	}

}
