package net.spy.memcached.protocol.binary;

import net.spy.memcached.ops.CASOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.StoreType;

class StoreOperationImpl extends SingleKeyOperationImpl
	implements StoreOperation, CASOperation {

	private static final byte SET=0x01;
	private static final byte ADD=0x02;
	private static final byte REPLACE=0x03;

	static final byte SETQ=0x11;
	static final byte ADDQ=0x12;
	static final byte REPLACEQ=0x13;

	// 4-byte flags, 4-byte expiration
	static final int EXTRA_LEN = 8;

	private final StoreType storeType;
	private final int flags;
	private final int exp;
	private final long cas;
	private final byte[] data;

	private static byte cmdMap(StoreType t) {
		byte rv=DUMMY_OPCODE;
		switch(t) {
			case set: rv=SET; break;
			case add: rv=ADD; break;
			case replace: rv=REPLACE; break;
		}
		// Check fall-through.
		assert rv != DUMMY_OPCODE : "Unhandled store type:  " + t;
		return rv;
	}

	public StoreOperationImpl(StoreType t, String k, int f, int e,
			byte[] d, long c, OperationCallback cb) {
		super(cmdMap(t), generateOpaque(), k, cb);
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

	@Override
	public String toString() {
		return super.toString() + " Cas: " + cas + " Exp: " + exp + " Flags: "
			+ flags + " Data Length: " + data.length;
	}
}
