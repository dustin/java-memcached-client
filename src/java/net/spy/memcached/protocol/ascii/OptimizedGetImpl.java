package net.spy.memcached.protocol.ascii;

import java.util.HashSet;

import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.protocol.ProxyCallback;

/**
 * Optimized Get operation for folding a bunch of gets together.
 */
final class OptimizedGetImpl extends GetOperationImpl {

	private final ProxyCallback pcb;

	/**
	 * Construct an optimized get starting with the given get operation.
	 */
	public OptimizedGetImpl(GetOperation firstGet) {
		super(new HashSet<String>(), new ProxyCallback());
		pcb=(ProxyCallback)getCallback();
		addOperation(firstGet);
	}

	/**
	 * Add a new GetOperation to get.
	 */
	public void addOperation(GetOperation o) {
		getKeys().addAll(o.getKeys());
		pcb.addCallbacks(o);
	}
}
