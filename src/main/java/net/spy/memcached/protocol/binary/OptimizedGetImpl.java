package net.spy.memcached.protocol.binary;

import java.util.Collections;

import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.protocol.ProxyCallback;

/**
 * Optimized Get operation for folding a bunch of gets together.
 */
final class OptimizedGetImpl extends MultiGetOperationImpl {

	private final ProxyCallback pcb;

	/**
	 * Construct an optimized get starting with the given get operation.
	 */
	public OptimizedGetImpl(GetOperation firstGet) {
		super(Collections.<String>emptySet(), new ProxyCallback());
		pcb=(ProxyCallback)getCallback();
		addOperation(firstGet);
	}

	/**
	 * Add a new GetOperation to get.
	 */
	public void addOperation(GetOperation o) {
		pcb.addCallbacks(o);
		for(String k : o.getKeys()) {
			addKey(k);
		}
	}

}
