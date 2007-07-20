// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.protocol.ascii;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.spy.SpyObject;

/**
 * Operations on a memcached connection.
 */
public abstract class OperationImpl extends SpyObject {
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
	 * Error classification.
	 */
	public enum ErrorType {
		/**
		 * General error.
		 */
		GENERAL(0),
		/**
		 * Error that occurred because the client did something stupid.
		 */
		CLIENT("CLIENT_ERROR ".length()),
		/**
		 * Error that occurred because the server did something stupid.
		 */
		SERVER("SERVER_ERROR ".length());

		private final int size;

		ErrorType(int s) {
			size=s;
		}

		public int getSize() {
			return size;
		}
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

	protected static final byte[] CRLF={'\r', '\n'};

	private final StringBuilder currentLine=new StringBuilder();
	private State state=State.WRITING;
	private ReadType readType=ReadType.LINE;
	private ByteBuffer cmd=null;
	private boolean cancelled=false;
	private boolean foundCr=false;
	private OperationException exception=null;
	private OperationCallback callback=null;

	protected OperationImpl() {
		super();
	}

	protected OperationImpl(OperationCallback cb) {
		super();
		callback=cb;
	}

	/**
	 * Get the operation callback associated with this operation.
	 */
	protected OperationCallback getCallback() {
		return callback;
	}


	/**
	 * Set the callback for this instance.
	 */
	protected void setCallback(OperationCallback to) {
		callback=to;
	}

	/**
	 * Has this operation been cancelled?
	 */
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * True if an error occurred while processing this operation.
	 */
	public boolean hasErrored() {
		return exception != null;
	}

	/**
	 * Get the exception that occurred (or null if no exception occurred).
	 */
	public OperationException getException() {
		return exception;
	}

	/**
	 * Cancel this operation.
	 */
	public void cancel() {
		cancelled=true;
		wasCancelled();
		callback.complete();
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
		if(state == State.COMPLETE) {
			callback.complete();
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
		bb.put(CRLF);
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
	public final void readFromBuffer(ByteBuffer data) throws IOException {
		// Loop while there's data remaining to get it all drained.
		while(state != State.COMPLETE && data.remaining() > 0) {
			if(readType == ReadType.DATA) {
				handleRead(data);
			} else {
				int offset=-1;
				for(int i=0; data.remaining() > 0; i++) {
					byte b=data.get();
					if(b == '\r') {
						foundCr=true;
					} else if(b == '\n') {
						assert foundCr: "got a \\n without a \\r";
						offset=i;
						foundCr=false;
						break;
					} else {
						assert !foundCr : "got a \\r without a \\n";
						currentLine.append((char)b);
					}
				}
				if(offset >= 0) {
					String line=currentLine.toString();
					currentLine.delete(0, currentLine.length());
					assert currentLine.length() == 0;
					ErrorType eType=classifyError(line);
					if(eType != null) {
						handleError(eType, line);
					} else {
						handleLine(line);
					}
				}
			}
		}
	}

	private void handleError(ErrorType eType, String line) throws IOException {
		getLogger().error("Error:  %s", line);
		switch(eType) {
			case GENERAL:
				exception=new OperationException();
				break;
			case SERVER:
				exception=new OperationException(eType, line);
				break;
			case CLIENT:
				exception=new OperationException(eType, line);
				break;
			default: assert false;
		}
		transitionState(State.COMPLETE);
		throw exception;
	}

	private ErrorType classifyError(String line) {
		ErrorType rv=null;
		if(line.startsWith("ERROR")) {
			rv=ErrorType.GENERAL;
		} else if(line.startsWith("CLIENT_ERROR")) {
			rv=ErrorType.CLIENT;
		} else if(line.startsWith("SERVER_ERROR")) {
			rv=ErrorType.SERVER;
		}
		return rv;
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
