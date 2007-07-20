// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.protocol.ascii;

import java.nio.ByteBuffer;

import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;

/**
 * Operation to delete an item from the cache.
 */
final class DeleteOperationImpl extends OperationImpl
	implements DeleteOperation {

	private static final int OVERHEAD=32;

	private final String key;
	private final int when;

	public DeleteOperationImpl(String k, int w, OperationCallback cb) {
		super(cb);
		key=k;
		when=w;
	}

	@Override
	public void handleLine(String line) {
		getLogger().debug("Delete of %s returned %s", key, line);
		assert line.equals("DELETED") || line.equals("NOT_FOUND");
		getCallback().receivedStatus(line);
		transitionState(OperationState.COMPLETE);
	}

	@Override
	public void initialize() {
		ByteBuffer b=ByteBuffer.allocate(key.length() + OVERHEAD);
		setArguments(b, "delete", key, when);
		b.flip();
		setBuffer(b);
	}

}
