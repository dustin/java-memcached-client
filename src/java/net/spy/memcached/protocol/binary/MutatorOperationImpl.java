package net.spy.memcached.protocol.binary;

import net.spy.memcached.ops.MutatatorOperation;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;

public class MutatorOperationImpl extends OperationImpl implements
		MutatatorOperation {

	private static final int CMD_INCR=5;
	private static final int CMD_DECR=6;

	private final String key;
	private final int by;
	private final int exp;
	private final long def;

	public MutatorOperationImpl(Mutator m, String k, int b,
			long d, int e, OperationCallback cb) {
		super(getCmd(m), generateOpaque(), cb);
		assert d >= 0 : "Default value is below zero";
		key=k;
		by=b;
		exp=e;
		def=d;
	}

	private static int getCmd(Mutator m) {
		int rv=0;
		switch(m) {
			case decr: rv=CMD_DECR; break;
			case incr: rv=CMD_INCR; break;
			default: assert false;
		}
		return rv;
	}

	@Override
	public void initialize() {
		// We're passing around a long so we can cover an unsigned integer.
		byte[] defBytes=new byte[4];
		defBytes[0]=(byte)((def >> 24) & 0xff);
		defBytes[1]=(byte)((def >> 16) & 0xff);
		defBytes[2]=(byte)((def >> 8) & 0xff);
		defBytes[3]=(byte)(def & 0xff);
		prepareBuffer(key, EMPTY_BYTES, by, defBytes, exp);
	}

	@Override
	protected OperationStatus getStatusForErrorCode(int errCode, byte[] errPl) {
		return errCode == ERR_NOT_FOUND ? NOT_FOUND_STATUS : null;
	}

	@Override
	protected void decodePayload(byte[] pl) {
		assert pl.length == 4 : "expected 4 bytes, got " + pl.length;
		getCallback().receivedStatus(new OperationStatus(true,
				String.valueOf(decodeInt(pl, 0))));
	}

}
