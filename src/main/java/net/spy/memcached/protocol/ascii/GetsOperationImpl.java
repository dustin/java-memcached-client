package net.spy.memcached.protocol.ascii;

import java.util.Collections;

import net.spy.memcached.ops.GetsOperation;

/**
 * Implementation of the gets operation.
 */
class GetsOperationImpl extends BaseGetOpImpl implements GetsOperation {

	private static final String CMD="gets";

	public GetsOperationImpl(String key, GetsOperation.Callback cb) {
		super(CMD, cb, Collections.singleton(key));
	}

}
