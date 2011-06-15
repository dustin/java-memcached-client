package net.spy.memcached.protocol.binary;

import java.util.Collection;
import java.util.Collections;

import net.spy.memcached.ops.KeyedOperation;
import net.spy.memcached.ops.OperationCallback;

/**
 * Operation to reset a timeout in Membase server.
 */
public class TouchOperationImpl extends OperationImpl
	implements KeyedOperation {

	static final int CMD=0x1c;

	private final String key;
	private final int exp;

	protected TouchOperationImpl(String k, int e, OperationCallback cb) {
		super(CMD, generateOpaque(), cb);
		key=k;
		exp=e;
	}

	@Override
	public Collection<String> getKeys() {
		return Collections.singleton(key);
	}

	@Override
	public void initialize() {
		prepareBuffer(key, 0, EMPTY_BYTES, exp);
	}

}
