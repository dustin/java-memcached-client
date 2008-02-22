package net.spy.memcached.protocol.ascii;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import net.spy.memcached.ops.GetsOperation;

/**
 * Implementation of the gets operation.
 */
public class GetsOperationImpl extends BaseGetOpImpl implements GetsOperation {

	public GetsOperationImpl(String key, GetsOperation.Callback cb) {
		super(cb, Collections.singleton(key));
	}

	public GetsOperationImpl(Collection<String> k, GetsOperation.Callback c) {
		super(c, new HashSet<String>(k));
	}

	@Override
	protected String getCmd() {
		return "gets";
	}

}
