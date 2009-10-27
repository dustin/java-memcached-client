package net.spy.memcached.protocol.binary;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.spy.memcached.KeyUtil;
import net.spy.memcached.ops.CASOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StoreType;

public class OptimizedSetImpl extends OperationImpl implements Operation {

	private static final OperationCallback NOOP_CALLBACK = new NoopCallback();

	private final int terminalOpaque=generateOpaque();
	private final Map<Integer, OperationCallback> callbacks =
		new HashMap<Integer, OperationCallback>();
	private final List<CASOperation> ops = new ArrayList<CASOperation>();

	// If nothing else, this will be a NOOP.
	private int byteCount = MIN_RECV_PACKET;

	/**
	 * Construct an optimized get starting with the given get operation.
	 */
	public OptimizedSetImpl(CASOperation firstStore) {
		super(-1, -1, NOOP_CALLBACK);
		addOperation(firstStore);
	}

	public void addOperation(CASOperation op) {
		ops.add(op);

		// Count the bytes required by this operation.
		Iterator<String> is = op.getKeys().iterator();
		String k = is.next();
		int keylen = KeyUtil.getKeyBytes(k).length;

		byteCount += MIN_RECV_PACKET + StoreOperationImpl.EXTRA_LEN
			+ keylen + op.getBytes().length;
	}

	public int size() {
		return ops.size();
	}

	public int bytes() {
		return byteCount;
	}

	@Override
	public void initialize() {
		// Now create a buffer.
		ByteBuffer bb=ByteBuffer.allocate(byteCount);
		for(CASOperation so : ops) {
			Iterator<String> is = so.getKeys().iterator();
			String k = is.next();
			byte[] keyBytes = KeyUtil.getKeyBytes(k);
			assert !is.hasNext();

			int myOpaque = generateOpaque();
			callbacks.put(myOpaque, so.getCallback());
			byte[] data = so.getBytes();

			// Custom header
			bb.put(REQ_MAGIC);
			bb.put((byte)cmdMap(so.getStoreType()));
			bb.putShort((short)keyBytes.length);
			bb.put((byte)StoreOperationImpl.EXTRA_LEN); // extralen
			bb.put((byte)0); // data type
			bb.putShort((short)0); // reserved
			bb.putInt(keyBytes.length + data.length +
						StoreOperationImpl.EXTRA_LEN);
			bb.putInt(myOpaque);
			bb.putLong(so.getCasValue()); // cas
			// Extras
			bb.putInt(so.getFlags());
			bb.putInt(so.getExpiration());
			// the actual key
			bb.put(keyBytes);
			// And the value
			bb.put(data);
		}
		// Add the noop
		bb.put(REQ_MAGIC);
		bb.put((byte)NoopOperationImpl.CMD);
		bb.putShort((short)0);
		bb.put((byte)0); // extralen
		bb.put((byte)0); // data type
		bb.putShort((short)0); // reserved
		bb.putInt(0);
		bb.putInt(terminalOpaque);
		bb.putLong(0); // cas

		bb.flip();
		setBuffer(bb);
	}

	private static int cmdMap(StoreType t) {
		int rv=-1;
		switch(t) {
			case set: rv=StoreOperationImpl.SETQ; break;
			case add: rv=StoreOperationImpl.ADDQ; break;
			case replace: rv=StoreOperationImpl.REPLACEQ; break;
		}
		// Check fall-through.
		assert rv != -1 : "Unhandled store type:  " + t;
		return rv;
	}

	@Override
	protected void finishedPayload(byte[] pl) throws IOException {
		if(responseOpaque == terminalOpaque) {
			for(OperationCallback cb : callbacks.values()) {
				cb.receivedStatus(STATUS_OK);
				cb.complete();
			}
			transitionState(OperationState.COMPLETE);
		} else {
			OperationCallback cb = callbacks.remove(responseOpaque);
			assert cb != null : "No callback for " + responseOpaque;
			assert errorCode != 0 : "Got no error on a quiet mutation.";
			OperationStatus status=getStatusForErrorCode(errorCode, pl);
			assert status != null : "Got no status for a quiet mutation error";
			cb.receivedStatus(status);
			cb.complete();
		}
		resetInput();
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

	@Override
	protected boolean opaqueIsValid() {
		return responseOpaque == terminalOpaque
			|| callbacks.containsKey(responseOpaque);
	}

	static class NoopCallback implements OperationCallback {

		public void complete() {
			// noop
		}

		public void receivedStatus(OperationStatus status) {
			// noop
		}

	}

}
