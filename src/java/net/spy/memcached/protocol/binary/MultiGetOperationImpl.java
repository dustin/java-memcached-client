package net.spy.memcached.protocol.binary;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;

class MultiGetOperationImpl extends OperationImpl implements GetOperation {

	private static final int CMD_GETQ=8;

	private final Map<Integer, String> keys=new HashMap<Integer, String>();
	private final Map<String, Integer> rkeys=new HashMap<String, Integer>();

	private final int terminalOpaque=generateOpaque();

	public MultiGetOperationImpl(Collection<String> k, OperationCallback cb) {
		super(-1, -1, cb);
		for(String s : new HashSet<String>(k)) {
			addKey(s);
		}
	}

	/**
	 * Add a key (and return its new opaque value).
	 */
	protected int addKey(String k) {
		Integer rv=rkeys.get(k);
		if(rv == null) {
			rv=generateOpaque();
			keys.put(rv, k);
			rkeys.put(k, rv);
		}
		return rv;
	}

	@Override
	public void initialize() {
		int size=(1+keys.size()) * MIN_RECV_PACKET;
		for(String s : keys.values()) {
			size += s.length();
		}
		// set up the initial header stuff
		ByteBuffer bb=ByteBuffer.allocate(size);
		for(Map.Entry<Integer, String> me : keys.entrySet()) {
			String key=me.getValue();
			bb.put(MAGIC);
			bb.put((byte)CMD_GETQ);
			bb.put((byte)key.length());
			bb.put((byte)0);
			bb.putInt(me.getKey());
			bb.putInt(key.length());
			bb.put(key.getBytes());
		}
		// Add the noop
		bb.put(MAGIC);
		bb.put((byte)NoopOperationImpl.CMD);
		bb.put((byte)0);
		bb.put((byte)0);
		bb.putInt(terminalOpaque);
		bb.putInt(0);

		bb.flip();
		setBuffer(bb);
	}

	@Override
	protected void finishedPayload(byte[] pl) throws IOException {
		if(responseOpaque == terminalOpaque) {
			getCallback().receivedStatus(STATUS_OK);
			transitionState(OperationState.COMPLETE);
		} else if(errorCode != 0) {
			getLogger().warn("Error on key %s:  %s (%d)",
				keys.get(responseOpaque), new String(pl), errorCode);
		} else {
			final int flags=decodeInt(pl, 0);
			final byte[] data=new byte[pl.length - 4];
			System.arraycopy(pl, 4, data, 0, pl.length-4);
			Callback cb=(Callback)getCallback();
			cb.gotData(keys.get(responseOpaque), flags, data);
		}
		resetInput();
	}

	@Override
	protected boolean opaqueIsValid() {
		return responseOpaque == terminalOpaque
			|| keys.containsKey(responseOpaque);
	}

	public Collection<String> getKeys() {
		return keys.values();
	}

}
