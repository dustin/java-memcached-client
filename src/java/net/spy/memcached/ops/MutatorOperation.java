// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 3946519E-AE99-49A5-A5C8-5A0E43AF6F8B

package net.spy.memcached.ops;

import java.nio.ByteBuffer;

/**
 * Operation for mutating integers inside of memcached.
 */
public class MutatorOperation extends Operation {

	public static final int OVERHEAD=32;

	public enum Mutator { incr, decr }

	private Mutator mutator=null;
	private String key=null;
	private int amount=0;
	private Callback cb=null;

	public MutatorOperation(Mutator m, String k, int amt, Callback c) {
		super();
		mutator=m;
		key=k;
		amount=amt;
		cb=c;
	}

	@Override
	public void handleLine(String line) {
		getLogger().debug("Result:  %s", line);
		Long found=null;
		if(!line.equals("NOT_FOUND")) {
			found=new Long(line);
		}
		if(cb != null) {
			cb.mutatorResult(found);
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

	public interface Callback {
		void mutatorResult(Long val);
	}

}
