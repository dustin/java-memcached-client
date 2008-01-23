// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.protocol.ascii;

import java.nio.ByteBuffer;

import net.spy.memcached.KeyUtil;
import net.spy.memcached.ops.DeleteOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;

/**
 * Operation to delete an item from the cache.
 */
final class DeleteOperationImpl extends OperationImpl
	implements DeleteOperation {

	private static final int OVERHEAD=32;

	private static final OperationStatus DELETED=
		new OperationStatus(true, "DELETED");
	private static final OperationStatus NOT_FOUND=
		new OperationStatus(false, "NOT_FOUND");

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
		getCallback().receivedStatus(matchStatus(line, DELETED, NOT_FOUND));
		transitionState(OperationState.COMPLETE);
	}

	@Override
	public void initialize() {
		ByteBuffer b=ByteBuffer.allocate(
			KeyUtil.getKeyBytes(key).length + OVERHEAD);
		setArguments(b, "delete", key, when);
		b.flip();
		setBuffer(b);
	}

}
