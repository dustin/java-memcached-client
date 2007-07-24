package net.spy.memcached.protocol.binary;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationErrorType;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.protocol.BaseOperationImpl;

/**
 * Base class for binary operations.
 */
abstract class OperationImpl extends BaseOperationImpl {

	// Base response packet format:
	//  magic (8-bits)
	//  cmd   (8 bits)
	//  error code (8 bits)
	//  reserved (8 bits)
	//  opaque (32-bits)
	//  key length (32-bits)

	private static final byte MAGIC = 0xf;
	private static final int MIN_RECV_PACKET=12;

	/**
	 * Error code for items that were not found.
	 */
	protected static final int NOT_FOUND = 1;

	protected static final byte[] EMPTY_BYTES = new byte[0];

	protected static final OperationStatus STATUS_OK =
		new OperationStatus(true, "OK");

	private static final AtomicInteger seqNumber=new AtomicInteger(0);

	// request header fields
	private final int cmd;
	protected final int opaque;

	private final byte[] header=new byte[MIN_RECV_PACKET];
	private int headerOffset=0;
	private byte[] payload=null;

	// Response header fields
	private int responseCmd=0;
	private int errorCode=0;
	private int responseOpaque;

	private int payloadOffset=0;

	/**
	 * Construct with opaque.
	 *
	 * @param o the opaque value.
	 * @param cb
	 */
	protected OperationImpl(int c, int o, OperationCallback cb) {
		super();
		cmd=c;
		opaque=o;
		setCallback(cb);
	}

	@Override
	public void readFromBuffer(ByteBuffer b) throws IOException {
		// First process headers if we haven't completed them yet
		if(headerOffset < MIN_RECV_PACKET) {
			int toRead=MIN_RECV_PACKET - headerOffset;
			int available=b.remaining();
			toRead=Math.min(toRead, available);
			getLogger().debug("Reading %d header bytes", toRead);
			b.get(header, headerOffset, toRead);
			headerOffset+=toRead;

			// We've completed reading the header.  Prepare body read.
			if(headerOffset == MIN_RECV_PACKET) {
				int magic=header[0];
				assert magic == MAGIC : "Invalid magic:  " + magic;
				responseCmd=header[1];
				assert responseCmd == cmd : "Unexpected response command value";
				errorCode=header[2];
				assert header[3] == 0 : "Reserved byte was not 0";
				responseOpaque=decodeInt(header, 4);
				assert opaqueIsValid() : "Opaque is not valid";
				int bytesToRead=decodeInt(header, 8);
				payload=new byte[bytesToRead];
			}
		}

		// Now process the payload if we can.
		if(headerOffset >= MIN_RECV_PACKET && payload == null) {
			finishedPayload(EMPTY_BYTES);
		} else {
			int toRead=payload.length - payloadOffset;
			int available=b.remaining();
			toRead=Math.min(toRead, available);
			getLogger().debug("Reading %d payload bytes", toRead);
			b.get(payload, payloadOffset, toRead);
			payloadOffset+=toRead;

			// Have we read it all?
			if(payloadOffset == payload.length) {
				finishedPayload(payload);
			} else {
				System.err.printf("Have read %d of %d bytes\n",
						payloadOffset, payload.length);
			}
		}

	}

	private void finishedPayload(byte[] pl) throws IOException {
		if(errorCode != 0) {
			handleError(errorCode, pl);
		} else {
			decodePayload(pl);
			transitionState(OperationState.COMPLETE);
		}
	}

	/**
	 * Handle a non-zero status code.
	 *
	 * @param errCode the status code
	 * @param errPl the payload that came with the non-zero status code
	 * @throws IOException if you don't like it.
	 */
	protected void handleError(int errCode, byte[] errPl) throws IOException {
		handleError(OperationErrorType.SERVER, new String(errPl));
	}

	/**
	 * Decode the given payload for this command.
	 *
	 * @param pl the payload.
	 */
	protected void decodePayload(byte[] pl) {
		assert pl.length == 0 : "Payload has bytes, but decode isn't overridden";
		getCallback().receivedStatus(STATUS_OK);
	}

	/**
	 * Validate an opaque value from the header.
	 * This may be overridden from a subclass where the opaque isn't expected
	 * to always be the same as the request opaque.
	 */
	protected boolean opaqueIsValid() {
		if(responseOpaque != opaque) {
			System.err.printf("Expected opaque:  %d, got opaque:  %d\n",
					responseOpaque, opaque);
		}
		return responseOpaque == opaque;
	}

	static int decodeInt(byte[] data, int i) {
		return (data[i]  & 0xff) << 24
			| (data[i+1] & 0xff) << 16
			| (data[i+2] & 0xff) << 8
			| (data[i+3] & 0xff);
	}

	/**
	 * Prepare a send buffer.
	 *
	 * @param cmd the command identifier
	 * @param key the key (for keyed ops)
	 * @param val the data payload
	 * @param extraHeaders any additional headers that need to be sent
	 */
	protected void prepareBuffer(String key, byte[] val,
			Object... extraHeaders) {
		int extraLen=0;
		for(Object o : extraHeaders) {
			if(o instanceof Integer) {
				extraLen += 4;
			} else {
				assert false : "Unhandled extra header type:  " + o.getClass();
			}
		}
		int bufSize=MIN_RECV_PACKET + key.length() + val.length;

		// set up the initial header stuff
		ByteBuffer bb=ByteBuffer.allocate(bufSize + extraLen);
		bb.put(MAGIC);
		bb.put((byte)cmd);
		bb.put((byte)key.length());
		bb.put((byte)0);
		bb.putInt(opaque);
		bb.putInt(key.length() + val.length + extraLen);

		// Add the extra headers.
		for(Object o : extraHeaders) {
			if(o instanceof Integer) {
				bb.putInt((Integer)o);
			} else {
				assert false : "Unhandled extra header type:  " + o.getClass();
			}
		}

		// Add the normal stuff
		bb.put(key.getBytes());
		bb.put(val);

		bb.flip();
		setBuffer(bb);
	}

	/**
	 * Generate an opaque ID.
	 */
	static int generateOpaque() {
		int rv=seqNumber.incrementAndGet();
		while(rv < 0) {
			seqNumber.compareAndSet(rv, 0);
			rv=seqNumber.incrementAndGet();
		}
		return rv;
	}
}
