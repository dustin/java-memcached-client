package net.spy.memcached.protocol.ascii;

import net.spy.memcached.ops.GetlOperation;

/**
 * Implementation of the getl operation.
 */
class GetlOperationImpl extends BaseGetOpImpl implements GetlOperation {

	private static final String CMD="getl";

	public GetlOperationImpl(String key, int exp, GetlOperation.Callback c) {
		super(CMD, exp, c, key);
	}
}

