package net.spy.memcached.protocol.binary;

import java.util.Collection;
import java.util.Collections;

import net.spy.memcached.ops.KeyedOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;

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
	protected OperationStatus getStatusForErrorCode(int errCode, byte[] errPl) {
        OperationStatus baseStatus = super.getStatusForErrorCode(errCode, errPl);
        if (baseStatus != null) {
            return baseStatus;
        }
		OperationStatus rv=null;
		switch(errCode) {
			case ERR_EXISTS:
				rv=EXISTS_STATUS;
				break;
			case ERR_NOT_FOUND:
				rv=NOT_FOUND_STATUS;
				break;
			case ERR_TEMP_FAIL:
				rv=TEMP_FAIL;
				break;
		}
		return rv;
	}

	@Override
	public void initialize() {
		prepareBuffer(key, 0, EMPTY_BYTES, exp);
	}

}
