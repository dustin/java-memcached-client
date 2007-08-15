package net.spy.memcached.ops;

import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Base interface for all operations.
 */
public interface Operation {

	/**
	 * Has this operation been cancelled?
	 */
	boolean isCancelled();

	/**
	 * True if an error occurred while processing this operation.
	 */
	boolean hasErrored();

	/**
	 * Get the exception that occurred (or null if no exception occurred).
	 */
	OperationException getException();

	/**
	 * Get the callback for this get operation.
	 */
	OperationCallback getCallback();

	/**
	 * Cancel this operation.
	 */
	void cancel();

	/**
	 * Get the current state of this operation.
	 */
	OperationState getState();

	/**
	 * Get the write buffer for this operation.
	 */
	ByteBuffer getBuffer();

	/**
	 * Invoked after having written all of the bytes from the supplied output
	 * buffer.
	 */
	void writeComplete();

	/**
	 * Initialize this operation.  This is used to prepare output byte buffers
	 * and stuff.
	 */
	void initialize();

	/**
	 * Read data from the given byte buffer and dispatch to the appropriate
	 * read mechanism.
	 */
	void readFromBuffer(ByteBuffer data) throws IOException;

	/**
	 * Handle a raw data read.
	 */
	void handleRead(ByteBuffer data);

}
