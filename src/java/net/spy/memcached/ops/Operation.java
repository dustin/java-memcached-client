package net.spy.memcached.ops;

import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Base interface for all operations.
 */
public interface Operation {
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
	State getState();

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
	 * Get the current read type of this operation.
	 */
	ReadType getReadType();

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

	/**
	 * Handle a textual read.
	 */
	void handleLine(String line);

}
