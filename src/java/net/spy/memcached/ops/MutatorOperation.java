// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

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

	private final Mutator mutator;
	private final String key;
	private final int amount;

	public MutatorOperation(Mutator m, String k, int amt, OperationCallback c) {
		super(c);
		mutator=m;
		key=k;
		amount=amt;
	}

	@Override
	public void handleLine(String line) {
		getLogger().debug("Result:  %s", line);
		String found=null;
		if(!line.equals("NOT_FOUND")) {
			found=line;
		}
		getCallback().receivedStatus(found);
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

	@Override
	protected void wasCancelled() {
		// XXX:  Replace this comment with why the hell I did this.
		getCallback().receivedStatus("cancelled");
	}

}
