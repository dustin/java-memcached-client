// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 010123B7-2F77-4047-A78A-A7B43D50151C

package net.spy.memcached.ops;

import java.nio.ByteBuffer;

public class StoreOperation extends Operation {

	public enum StoreType { set, add, replace }

	// Overhead storage stuff to make sure the buffer pushes out far enough.
	private static final int OVERHEAD = 32;

	private StoreType type=null;
	private String key=null;
	private int flags=0;
	private int exp=0;
	private byte[] data=null;
	private Callback cb=null;

	public StoreOperation(StoreType t, String key, int flags, int exp,
			byte[] data, Callback callback) {
		super();
		this.type=t;
		this.key=key;
		this.flags=flags;
		this.exp=exp;
		this.data=data;
		this.cb=callback;
	}

	@Override
	public void handleLine(String firstLine) {
		if(cb != null) {
			cb.storeResult(firstLine);
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

	public interface Callback {

		/**
		 * Report the result of an asynchronous callback.
		 */
		public void storeResult(String val);
	}
}
