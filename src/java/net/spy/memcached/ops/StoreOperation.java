// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.ops;

import java.nio.ByteBuffer;

/**
 * Operation to store data in a memcached server.
 */
public class StoreOperation extends Operation {

	/**
	 * The type of storage operation to perform.
	 */
	public enum StoreType {
		/**
		 * Unconditionally store a value in the cache.
		 */
		set,
		/**
		 * Store a value in the cache iff there is not already something stored
		 * for the given key.
		 */
		add,
		/**
		 * Store a value in the cache iff there is already something stored for
		 * the given key.
		 */
		replace
	}

	// Overhead storage stuff to make sure the buffer pushes out far enough.
	private static final int OVERHEAD = 32;

	private StoreType type=null;
	private String key=null;
	private int flags=0;
	private int exp=0;
	private byte[] data=null;
	private OperationCallback cb=null;

	public StoreOperation(StoreType t, String k, int f, int e,
			byte[] d, OperationCallback callback) {
		super();
		this.type=t;
		this.key=k;
		this.flags=f;
		this.exp=e;
		this.data=d;
		this.cb=callback;
	}

	@Override
	public void handleLine(String line) {
		assert getState() == State.READING
			: "Read ``" + line + "'' when in " + getState() + " state";
		if(cb != null) {
			cb.receivedStatus(line);
		}
		transitionState(State.COMPLETE);
	}

	@Override
	public void initialize() {
		ByteBuffer bb=ByteBuffer.allocate(data.length + key.length()
				+ OVERHEAD);
		setArguments(bb, type.name(), key, flags, exp, data.length);
		assert bb.remaining() >= data.length + 2
			: "Not enough room in buffer, need another "
				+ (2 + data.length - bb.remaining());
		bb.put(data);
		bb.put("\r\n".getBytes());
		bb.flip();
		setBuffer(bb);
	}

	@Override
	protected void wasCancelled() {
		cb.receivedStatus("cancelled");
	}

}
