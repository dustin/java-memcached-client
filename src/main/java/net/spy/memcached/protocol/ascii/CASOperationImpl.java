package net.spy.memcached.protocol.ascii;

import java.nio.ByteBuffer;

import net.spy.memcached.CASResponse;
import net.spy.memcached.KeyUtil;
import net.spy.memcached.ops.CASOperation;
import net.spy.memcached.ops.CASOperationStatus;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;

class CASOperationImpl extends OperationImpl implements CASOperation {

	// Overhead storage stuff to make sure the buffer pushes out far enough.
	private static final int OVERHEAD = 32;

	private static final OperationStatus STORED=
		new CASOperationStatus(true, "STORED", CASResponse.OK);
	private static final OperationStatus NOT_FOUND=
		new CASOperationStatus(false, "NOT_FOUND", CASResponse.NOT_FOUND);
	private static final OperationStatus EXISTS=
		new CASOperationStatus(false, "EXISTS", CASResponse.EXISTS);

	private final String key;
	private final long casValue;
	private final int flags;
	private final byte[] data;

	public CASOperationImpl(String k, long c, int f,
			byte[] d, OperationCallback cb) {
		super(cb);
		key=k;
		casValue=c;
		flags=f;
		data=d;
	}

	@Override
	public void handleLine(String line) {
		assert getState() == OperationState.READING
			: "Read ``" + line + "'' when in " + getState() + " state";
		getCallback().receivedStatus(matchStatus(line,
			STORED, NOT_FOUND, EXISTS));
		transitionState(OperationState.COMPLETE);
	}

	@Override
	public void initialize() {
		ByteBuffer bb=ByteBuffer.allocate(data.length
				+ KeyUtil.getKeyBytes(key).length + OVERHEAD);
		setArguments(bb, "cas", key, flags, 0, data.length, casValue);
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
