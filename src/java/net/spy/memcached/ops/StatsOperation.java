// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 8671097A-797A-4FCA-A1A0-AC333C1DF32E

package net.spy.memcached.ops;

import java.nio.ByteBuffer;

/**
 * Operation to retrieve statistics from a memcached server.
 */
public class StatsOperation extends Operation {

	private static final byte[] MSG="stats\r\n".getBytes();

	private Callback cb=null;

	public StatsOperation(Callback c) {
		super();
		cb=c;
	}

	@Override
	public void handleLine(String line) {
		if(line.equals("END")) {
			if(cb != null) {
				cb.receivedStatus(line);
			}
			transitionState(State.COMPLETE);
		} else {
			String[] parts=line.split(" ");
			assert parts.length == 3;
			if(cb != null) {
				cb.gotStat(parts[1], parts[2]);
			}
		}
	}

	@Override
	public void initialize() {
		setBuffer(ByteBuffer.wrap(MSG));
	}

	/**
	 * Callback for stats operation.
	 */
	public interface Callback extends OperationCallback {
		/**
		 * Invoked once for every stat returned from the server.
		 * 
		 * @param name the name of the stat
		 * @param val the stat value.
		 */
		void gotStat(String name, String val);
	}

}
