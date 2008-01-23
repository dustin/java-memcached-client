// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.protocol.ascii;

import java.nio.ByteBuffer;

import net.spy.memcached.KeyUtil;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.ops.StoreType;

/**
 * Operation to store data in a memcached server.
 */
final class StoreOperationImpl extends OperationImpl
	implements StoreOperation {

	// Overhead storage stuff to make sure the buffer pushes out far enough.
	private static final int OVERHEAD = 32;

	private static final OperationStatus STORED=
		new OperationStatus(true, "STORED");

	private final StoreType type;
	private final String key;
	private final int flags;
	private final int exp;
	private final byte[] data;

	public StoreOperationImpl(StoreType t, String k, int f, int e,
			byte[] d, OperationCallback cb) {
		super(cb);
		type=t;
		key=k;
		flags=f;
		exp=e;
		data=d;
	}

	@Override
	public void handleLine(String line) {
		assert getState() == OperationState.READING
			: "Read ``" + line + "'' when in " + getState() + " state";
		getCallback().receivedStatus(matchStatus(line, STORED));
		transitionState(OperationState.COMPLETE);
	}

	@Override
	public void initialize() {
		ByteBuffer bb=ByteBuffer.allocate(data.length
				+ KeyUtil.getKeyBytes(key).length + OVERHEAD);
		setArguments(bb, type.name(), key, flags, exp, data.length);
		assert bb.remaining() >= data.length + 2
			: "Not enough room in buffer, need another "
				+ (2 + data.length - bb.remaining());
		bb.put(data);
		bb.put(CRLF);
		bb.flip();
		setBuffer(bb);
	}

	@Override
	protected void wasCancelled() {
		// XXX:  Replace this comment with why I did this
		getCallback().receivedStatus(CANCELLED);
	}

}
