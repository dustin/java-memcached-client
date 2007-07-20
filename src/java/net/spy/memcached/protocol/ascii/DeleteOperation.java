// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.protocol.ascii;

import java.nio.ByteBuffer;

/**
 * Operation to delete an item from the cache.
 */
public class DeleteOperation extends Operation {

	private static final int OVERHEAD=32;

	private final String key;
	private final int when;

	public DeleteOperation(String k, int w, OperationCallback cb) {
		super(cb);
		key=k;
		when=w;
	}

	@Override
	public void handleLine(String line) {
		getLogger().debug("Delete of %s returned %s", key, line);
		assert line.equals("DELETED") || line.equals("NOT_FOUND");
		getCallback().receivedStatus(line);
		transitionState(State.COMPLETE);
	}

	@Override
	public void initialize() {
		ByteBuffer b=ByteBuffer.allocate(key.length() + OVERHEAD);
		setArguments(b, "delete", key, when);
		b.flip();
		setBuffer(b);
	}

	@Override
	protected void wasCancelled() {
		// nothing -- no callback
	}

}
