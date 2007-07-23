package net.spy.memcached.protocol.binary;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.spy.memcached.protocol.BaseOperationImpl;

/**
 * Base class for binary operations.
 */
public abstract class OperationImpl extends BaseOperationImpl {

	// Base response packet format:
	//  magic (8-bits)
	//  cmd   (8 bits)
	//  error code (8 bits)
	//  reserved (8 bits)
	//  opaque (32-bits)
	//  key length (32-bits)

	private static final byte MAGIC = 0xf;
	private static final int MIN_RECV_PACKET=12;

	@Override
	public void readFromBuffer(ByteBuffer data) throws IOException {
		// TODO Auto-generated method stub
	}

	/**
	 * Prepare a send buffer.
	 *
	 * @param cmd the command identifier
	 * @param opaque the opaque value for this command
	 * @param key the key (for keyed ops)
	 * @param val the data payload
	 * @param extraHeaders any additional headers that need to be sent
	 */
	protected void prepareBuffer(int cmd, int opaque, String key, byte[] val,
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
}
