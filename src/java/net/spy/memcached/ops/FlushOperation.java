// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 408561E4-F050-4CEC-9F6A-419370F0EB77

package net.spy.memcached.ops;

import java.nio.ByteBuffer;

/**
 * Memcached flush_all operation.
 */
public class FlushOperation extends Operation {

	private static final byte[] FLUSH="flush_all\r\n".getBytes();
	private int delay=-1;

	public FlushOperation() {
		super();
	}

	public FlushOperation(int d) {
		super();
		delay=d;
	}

	@Override
	public void handleLine(String line) {
		assert line.equals("OK");
		getLogger().info("Flush completed successfully");
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
		}
		setBuffer(b);
	}
}
