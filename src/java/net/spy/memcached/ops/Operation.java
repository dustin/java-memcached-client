// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.ops;

import java.nio.ByteBuffer;

import net.spy.SpyObject;

/**
 * Operations on a memcached connection.
 */
public abstract class Operation extends SpyObject {
	/**
	 * State of this operation.
	 */
	public enum State {
		/**
		 * State indicating this operation is writing data to the server.
		 */
		WRITING,
		/**
		 * State indicating this operation is reading data from the server.
		 */
		READING,
		/**
		 * State indicating this operation is complete.
		 */
		COMPLETE
	}
	/**
	 * Data read types.
	 */
	public enum ReadType {
		/**
		 * Read type indicating an operation currently wants to read lines.
		 */
		LINE,
		/**
		 * Read type indicating an operation currently wants to read raw data.
		 */
		DATA
	}
	
	private State state=State.WRITING;
	private ReadType readType=ReadType.LINE;
	private ByteBuffer cmd=null;
	private StringBuilder currentLine=new StringBuilder();
	private boolean cancelled=false;

	/**
	 * Has this operation been cancelled?
	 */
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * Cancel this operation.
	 */
	public void cancel() {
		cancelled=true;
		wasCancelled();
	}

	/**
	 * This is called on each subclass whenever an operation was cancelled.
	 */
	protected abstract void wasCancelled();

	/**
	 * Get the current state of this operation.
	 */
	public State getState() {
		return state;
	}

	/**
	 * Get the write buffer for this operation.
	 */
	public ByteBuffer getBuffer() {
		assert cmd != null : "No output buffer.";
		return cmd;
	}

	/**
	 * Set the write buffer for this operation.
	 */
	protected void setBuffer(ByteBuffer to) {
		assert to != null : "Trying to set buffer to null";
		cmd=to;
		cmd.mark();
	}

	/**
	 * Transition the state of this operation to the given state.
	 */
	protected void transitionState(State newState) {
		getLogger().debug("Transitioned state from %s to %s", state, newState);
		state=newState;
		// Discard our buffer when we no longer need it.
		if(state != State.WRITING) {
			cmd=null;
		}
	}

	/**
	 * Invoked after having written all of the bytes from the supplied output
	 * buffer.
	 */
	public void writeComplete() {
		transitionState(State.READING);
	}

	/**
	 * Get the current read type of this operation.
	 */
	public ReadType getReadType() {
		return readType;
	}

	/**
	 * Set the read type of this operation.
	 */
	protected void setReadType(ReadType to) {
		readType=to;
	}

	/**
	 * Set some arguments for an operation into the given byte buffer.
	 */
	protected void setArguments(ByteBuffer bb, Object... args) {
		boolean wasFirst=true;
		for(Object o : args) {
			if(wasFirst) {
				wasFirst=false;
			} else {
				bb.put((byte)' ');
			}
			bb.put(String.valueOf(o).getBytes());
		}
		bb.put("\r\n".getBytes());
	}

	/**
	 * Initialize this operation.  This is used to prepare output byte buffers
	 * and stuff.
	 */
	public abstract void initialize();

	/**
	 * Read data from the given byte buffer and dispatch to the appropriate
	 * read mechanism.
	 */
	public final void readFromBuffer(ByteBuffer data) {
		// Loop while there's data remaining to get it all drained.
		while(data.remaining() > 0) {
			if(readType == ReadType.DATA) {
				handleRead(data);
			} else {
				int offset=-1;
				for(int i=0; data.remaining() > 0; i++) {
					byte b=data.get();
					if(b == '\r') {
						byte newline=data.get();
						assert newline == '\n' : "got a \\r without a \\n";
						offset=i;
						break;
					} else {
						currentLine.append((char)b);
					}
				}
				if(offset >= 0) {
					handleLine(currentLine.toString());
					currentLine.delete(0, currentLine.length());
					assert currentLine.length() == 0;
				}
			}
		}
	}

	/**
	 * Handle a raw data read.
	 */
	public void handleRead(ByteBuffer data) {
		assert false;
	}

	/**
	 * Handle a textual read.
	 */
	public abstract void handleLine(String line);
}
