package net.spy.memcached.protocol.ascii;

import java.nio.ByteBuffer;

import net.spy.memcached.KeyUtil;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;

/**
 * Base class for ascii store operations (add, set, replace, append, prepend).
 */
abstract class BaseStoreOperationImpl extends OperationImpl {

	private static final int OVERHEAD = 32;
	private static final OperationStatus STORED =
		new OperationStatus(true, "STORED");
	protected final String type;
	protected final String key;
	protected final int flags;
	protected final int exp;
	protected final byte[] data;

	public BaseStoreOperationImpl(String t, String k, int f, int e,
			byte[] d, OperationCallback cb) {
		super(cb);
		type=t;
		key=k;
		flags=f;
		exp=e;
		data=d;
	}

	@Override
	public void handleLine(String line) {
		assert getState() == OperationState.READING
			: "Read ``" + line + "'' when in " + getState() + " state";
		getCallback().receivedStatus(matchStatus(line, STORED));
		transitionState(OperationState.COMPLETE);
	}

	@Override
	public void initialize() {
		ByteBuffer bb=ByteBuffer.allocate(data.length
				+ KeyUtil.getKeyBytes(key).length + OVERHEAD);
		setArguments(bb, type, key, flags, exp, data.length);
		assert bb.remaining() >= data.length + 2
			: "Not enough room in buffer, need another "
				+ (2 + data.length - bb.remaining());
		bb.put(data);
		bb.put(CRLF);
		bb.flip();
		setBuffer(bb);
	}

	@Override
	protected void wasCancelled() {
		// XXX:  Replace this comment with why I did this
		getCallback().receivedStatus(CANCELLED);
	}

}