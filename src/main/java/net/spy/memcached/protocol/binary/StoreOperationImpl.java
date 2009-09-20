package net.spy.memcached.protocol.binary;

import java.util.Collection;
import java.util.Collections;

import net.spy.memcached.ops.CASOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.StoreType;

class StoreOperationImpl extends OperationImpl
	implements StoreOperation, CASOperation {

	private static final int SET=1;
	private static final int ADD=2;
	private static final int REPLACE=3;

	static final int SETQ=0x11;
	static final int ADDQ=0x12;
	static final int REPLACEQ=0x13;

	// 4-byte flags, 4-byte expiration
	static final int EXTRA_LEN = 8;

	private final String key;
	private final StoreType storeType;
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
			byte[] d, long c, OperationCallback cb) {
		super(cmdMap(t), generateOpaque(), cb);
		key=k;
		flags=f;
		exp=e;
		data=d;
		cas=c;
		storeType=t;
	}

	@Override
	public void initialize() {
		prepareBuffer(key, cas, data, flags, exp);
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

	public Collection<String> getKeys() {
		return Collections.singleton(key);
	}

	public byte[] getBytes() {
		return data;
	}

	public long getCasValue() {
		return cas;
	}

	public int getExpiration() {
		return exp;
	}

	public int getFlags() {
		return flags;
	}

	public byte[] getData() {
		return data;
	}

	public StoreType getStoreType() {
		return storeType;
	}

}
