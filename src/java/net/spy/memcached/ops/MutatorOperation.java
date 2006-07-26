// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 3946519E-AE99-49A5-A5C8-5A0E43AF6F8B

package net.spy.memcached.ops;

import java.nio.ByteBuffer;

/**
 * Operation for mutating integers inside of memcached.
 */
public class MutatorOperation extends Operation {

	public static final int OVERHEAD=32;

	/**
	 * Type of mutation to perform.
	 */
	public enum Mutator {
		/**
		 * Increment a value on the memcached server.
		 */
		incr,
		/**
		 * Decrement a value on the memcached server.
		 */
		decr
	}

	private Mutator mutator=null;
	private String key=null;
	private int amount=0;
	private OperationCallback cb=null;

	public MutatorOperation(Mutator m, String k, int amt, OperationCallback c) {
		super();
		mutator=m;
		key=k;
		amount=amt;
		cb=c;
	}

	@Override
	public void handleLine(String line) {
		getLogger().debug("Result:  %s", line);
		String found=null;
		if(!line.equals("NOT_FOUND")) {
			found=line;
		}
		if(cb != null) {
			cb.receivedStatus(found);
		}
		transitionState(State.COMPLETE);
	}

	@Override
	public void initialize() {
		int size=key.length() + OVERHEAD;
		ByteBuffer b=ByteBuffer.allocate(size);
		setArguments(b, mutator.name(), key, amount);
		b.flip();
		setBuffer(b);
	}

}
