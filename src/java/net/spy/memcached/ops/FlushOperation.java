// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.ops;

import java.nio.ByteBuffer;

/**
 * Memcached flush_all operation.
 */
public class FlushOperation extends Operation {

	private static final byte[] FLUSH="flush_all\r\n".getBytes();
	private int delay=-1;

	public FlushOperation(OperationCallback cb) {
		this(-1, cb);
	}

	public FlushOperation(int d, OperationCallback cb) {
		super(cb);
		delay=d;
	}

	@Override
	public void handleLine(String line) {
		assert line.equals("OK") : "Expected OK, was " + line;
		getLogger().debug("Flush completed successfully");
		getCallback().receivedStatus(line);
		transitionState(State.COMPLETE);
	}

	@Override
	public void initialize() {
		ByteBuffer b=null;
		if(delay == -1) {
			b=ByteBuffer.wrap(FLUSH);
		} else {
			b=ByteBuffer.allocate(32);
			b.put( ("flush_all " + delay + "\r\n").getBytes());
			b.flip();
		}
		setBuffer(b);
	}

	@Override
	protected void wasCancelled() {
		// nothing -- no callback
	}
}
