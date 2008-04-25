package net.spy.memcached.protocol.binary;

import net.spy.memcached.ops.MutatatorOperation;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;

class MutatorOperationImpl extends OperationImpl implements
		MutatatorOperation {

	private static final int CMD_INCR=5;
	private static final int CMD_DECR=6;

	private final String key;
	private final long by;
	private final int exp;
	private final long def;

	public MutatorOperationImpl(Mutator m, String k, long b,
			long d, int e, OperationCallback cb) {
		super(m == Mutator.incr ? CMD_INCR : CMD_DECR, generateOpaque(), cb);
		assert d >= 0 : "Default value is below zero";
		key=k;
		by=b;
		exp=e;
		def=d;
	}

	@Override
	public void initialize() {
		// We're passing around a long so we can cover an unsigned integer.
		byte[] defBytes=new byte[8];
		defBytes[0]=(byte)((def >> 56) & 0xff);
		defBytes[1]=(byte)((def >> 48) & 0xff);
		defBytes[2]=(byte)((def >> 40) & 0xff);
		defBytes[3]=(byte)((def >> 32) & 0xff);
		defBytes[4]=(byte)((def >> 24) & 0xff);
		defBytes[5]=(byte)((def >> 16) & 0xff);
		defBytes[6]=(byte)((def >> 8) & 0xff);
		defBytes[7]=(byte)(def & 0xff);
		prepareBuffer(key, 0, EMPTY_BYTES, by, defBytes, exp);
	}

	@Override
	protected OperationStatus getStatusForErrorCode(int errCode, byte[] errPl) {
		return errCode == ERR_NOT_FOUND ? NOT_FOUND_STATUS : null;
	}

	@Override
	protected void decodePayload(byte[] pl) {
		getCallback().receivedStatus(new OperationStatus(true,
			String.valueOf(decodeLong(pl, 0))));
	}

}
