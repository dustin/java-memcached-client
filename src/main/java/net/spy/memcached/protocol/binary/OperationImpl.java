package net.spy.memcached.protocol.binary;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;

import net.spy.memcached.CASResponse;
import net.spy.memcached.KeyUtil;
import net.spy.memcached.ops.CASOperationStatus;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationErrorType;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.protocol.BaseOperationImpl;

/**
 * Base class for binary operations.
 */
abstract class OperationImpl extends BaseOperationImpl {

	protected static final byte REQ_MAGIC = (byte)0x80;
	protected static final byte RES_MAGIC = (byte)0x81;
	protected static final int MIN_RECV_PACKET=24;

	/**
	 * Error code for items that were not found.
	 */
	protected static final int ERR_NOT_FOUND = 1;
	protected static final int ERR_EXISTS = 2;
	protected static final int ERR_NOT_STORED = 5;

	protected static final OperationStatus NOT_FOUND_STATUS =
		new CASOperationStatus(false, "Not Found", CASResponse.NOT_FOUND);
	protected static final OperationStatus EXISTS_STATUS =
		new CASOperationStatus(false, "Object exists", CASResponse.EXISTS);
	protected static final OperationStatus NOT_STORED_STATUS =
		new CASOperationStatus(false, "Not Stored", CASResponse.NOT_FOUND);

	protected static final byte[] EMPTY_BYTES = new byte[0];

	protected static final OperationStatus STATUS_OK =
		new CASOperationStatus(true, "OK", CASResponse.OK);

	private static final AtomicInteger seqNumber=new AtomicInteger(0);

	// request header fields
	private final int cmd;
	protected final int opaque;

	private final byte[] header=new byte[MIN_RECV_PACKET];
	private int headerOffset=0;
	private byte[] payload=null;

	// Response header fields
	protected int responseCmd;
	protected int errorCode;
	protected int responseOpaque;
	protected long responseCas;

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

	protected void resetInput() {
		payload=null;
		payloadOffset=0;
		headerOffset=0;
	}

	// Base response packet format:
	//    0      1       2  3    4         5         6  7    8 9 10 11
	//	# magic, opcode, keylen, extralen, datatype, status, bodylen,
	//    12,3,4,5  16
	//    opaque, cas
	//	RES_PKT_FMT=">BBHBBHIIQ"

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
				assert magic == RES_MAGIC : "Invalid magic:  " + magic;
				responseCmd=header[1];
				assert cmd == -1 || responseCmd == cmd
					: "Unexpected response command value";
				// TODO:  Examine extralen and datatype
				errorCode=decodeShort(header, 6);
				int bytesToRead=decodeInt(header, 8);
				payload=new byte[bytesToRead];
				responseOpaque=decodeInt(header, 12);
				responseCas=decodeLong(header, 16);
				assert opaqueIsValid() : "Opaque is not valid";
			}
		}

		// Now process the payload if we can.
		if(headerOffset >= MIN_RECV_PACKET && payload == null) {
			finishedPayload(EMPTY_BYTES);
		} else if(payload != null) {
			int toRead=payload.length - payloadOffset;
			int available=b.remaining();
			toRead=Math.min(toRead, available);
			getLogger().debug("Reading %d payload bytes", toRead);
			b.get(payload, payloadOffset, toRead);
			payloadOffset+=toRead;

			// Have we read it all?
			if(payloadOffset == payload.length) {
				finishedPayload(payload);
			}
		} else {
			// Haven't read enough to make up a payload.  Must read more.
			getLogger().debug("Only read %d of the %d needed to fill a header",
				headerOffset, MIN_RECV_PACKET);
		}

	}

	protected void finishedPayload(byte[] pl) throws IOException {
		if(errorCode != 0) {
			OperationStatus status=getStatusForErrorCode(errorCode, pl);
			if(status == null) {
				handleError(OperationErrorType.SERVER, new String(pl));
			} else {
				getCallback().receivedStatus(status);
				transitionState(OperationState.COMPLETE);
			}
		} else {
			decodePayload(pl);
			transitionState(OperationState.COMPLETE);
		}
	}

	/**
	 * Get the OperationStatus object for the given error code.
	 *
	 * @param errCode the error code
	 * @return the status to return, or null if this is an exceptional case
	 */
	protected OperationStatus getStatusForErrorCode(int errCode, byte[] errPl) {
		return null;
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
			getLogger().warn("Expected opaque:  %d, got opaque:  %d\n",
					responseOpaque, opaque);
		}
		return responseOpaque == opaque;
	}

	static int decodeShort(byte[] data, int i) {
		return (data[i] & 0xff) << 8
			| (data[i+1] & 0xff);
	}

	static int decodeInt(byte[] data, int i) {
		return (data[i]  & 0xff) << 24
			| (data[i+1] & 0xff) << 16
			| (data[i+2] & 0xff) << 8
			| (data[i+3] & 0xff);
	}

	static long decodeUnsignedInt(byte[] data, int i) {
		return ((long)(data[i]  & 0xff) << 24)
			| ((data[i+1] & 0xff) << 16)
			| ((data[i+2] & 0xff) << 8)
			| (data[i+3] & 0xff);
	}

	static long decodeLong(byte[] data, int i) {
		return(data[i  ] & 0xff) << 56
			| (data[i+1] & 0xff) << 48
			| (data[i+2] & 0xff) << 40
			| (data[i+3] & 0xff) << 32
			| (data[i+4] & 0xff) << 24
			| (data[i+5] & 0xff) << 16
			| (data[i+6] & 0xff) << 8
			| (data[i+7] & 0xff);
	}

	/**
	 * Prepare a send buffer.
	 *
	 * @param cmd the command identifier
	 * @param key the key (for keyed ops)
	 * @param val the data payload
	 * @param extraHeaders any additional headers that need to be sent
	 */
	protected void prepareBuffer(String key, long cas, byte[] val,
			Object... extraHeaders) {
		int extraLen=0;
		for(Object o : extraHeaders) {
			if(o instanceof Integer) {
				extraLen += 4;
			} else if(o instanceof byte[]) {
				extraLen += ((byte[])o).length;
			} else if(o instanceof Long) {
				extraLen += 8;
			} else {
				assert false : "Unhandled extra header type:  " + o.getClass();
			}
		}
		final byte[] keyBytes=KeyUtil.getKeyBytes(key);
		int bufSize=MIN_RECV_PACKET + keyBytes.length + val.length;

		//	# magic, opcode, keylen, extralen, datatype, [reserved],
		//    bodylen, opaque, cas
		//	REQ_PKT_FMT=">BBHBBxxIIQ"

		// set up the initial header stuff
		ByteBuffer bb=ByteBuffer.allocate(bufSize + extraLen);
		assert bb.order() == ByteOrder.BIG_ENDIAN;
		bb.put(REQ_MAGIC);
		bb.put((byte)cmd);
		bb.putShort((short)keyBytes.length);
		bb.put((byte)extraLen);
		bb.put((byte)0); // data type
		bb.putShort((short)0); // reserved
		bb.putInt(keyBytes.length + val.length + extraLen);
		bb.putInt(opaque);
		bb.putLong(cas);

		// Add the extra headers.
		for(Object o : extraHeaders) {
			if(o instanceof Integer) {
				bb.putInt((Integer)o);
			} else if(o instanceof byte[]) {
				bb.put((byte[])o);
			} else if(o instanceof Long) {
				bb.putLong((Long)o);
			} else {
				assert false : "Unhandled extra header type:  " + o.getClass();
			}
		}

		// Add the normal stuff
		bb.put(keyBytes);
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
			if(seqNumber.compareAndSet(rv, 0)) {
				rv=seqNumber.incrementAndGet();
			}
		}
		return rv;
	}
}
