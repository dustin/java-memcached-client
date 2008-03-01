package net.spy.memcached.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.spy.SpyObject;
import net.spy.memcached.ops.CancelledOperationStatus;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationErrorType;
import net.spy.memcached.ops.OperationException;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;

/** 
 * Base class for protocol-specific operation implementations.
 */
public abstract class BaseOperationImpl extends SpyObject {

	/**
	 * Status object for cancelled operations.
	 */
	public static final OperationStatus CANCELLED =
		new CancelledOperationStatus();
	private OperationState state = OperationState.WRITING;
	private ByteBuffer cmd = null;
	private boolean cancelled = false;
	private OperationException exception = null;
	protected OperationCallback callback = null;

	public BaseOperationImpl() {
		super();
	}

	/**
	 * Get the operation callback associated with this operation.
	 */
	public final OperationCallback getCallback() {
		return callback;
	}

	/**
	 * Set the callback for this instance.
	 */
	protected void setCallback(OperationCallback to) {
		callback=to;
	}

	public final boolean isCancelled() {
		return cancelled;
	}

	public final boolean hasErrored() {
		return exception != null;
	}

	public final OperationException getException() {
		return exception;
	}

	public final void cancel() {
		cancelled=true;
		wasCancelled();
		callback.complete();
	}

	/**
	 * This is called on each subclass whenever an operation was cancelled.
	 */
	protected void wasCancelled() {
		getLogger().debug("was cancelled.");
	}

	public final OperationState getState() {
		return state;
	}

	public final ByteBuffer getBuffer() {
		assert cmd != null : "No output buffer.";
		return cmd;
	}

	/**
	 * Set the write buffer for this operation.
	 */
	protected final void setBuffer(ByteBuffer to) {
		assert to != null : "Trying to set buffer to null";
		cmd=to;
		cmd.mark();
	}

	/**
	 * Transition the state of this operation to the given state.
	 */
	protected final void transitionState(OperationState newState) {
		getLogger().debug("Transitioned state from %s to %s", state, newState);
		state=newState;
		// Discard our buffer when we no longer need it.
		if(state != OperationState.WRITING) {
			cmd=null;
		}
		if(state == OperationState.COMPLETE) {
			callback.complete();
		}
	}

	public final void writeComplete() {
		transitionState(OperationState.READING);
	}

	public abstract void initialize();

	public abstract void readFromBuffer(ByteBuffer data) throws IOException;

	protected void handleError(OperationErrorType eType, String line)
		throws IOException {
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
		transitionState(OperationState.COMPLETE);
		throw exception;
	}

	public void handleRead(ByteBuffer data) {
		assert false;
	}

}
