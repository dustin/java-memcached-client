package net.spy.memcached.protocol.binary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.protocol.GetCallbackWrapper;

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

	public static class ProxyCallback implements GetOperation.Callback {

		private final Map<String, Collection<GetOperation.Callback>> callbacks=
			new HashMap<String, Collection<GetOperation.Callback>>();
		private final Collection<GetOperation.Callback> allCallbacks=
			new ArrayList<GetOperation.Callback>();

		public void addCallbacks(GetOperation o) {
			GetOperation.Callback c=new GetCallbackWrapper(o.getKeys().size(),
					(GetOperation.Callback)o.getCallback());
			allCallbacks.add(c);
			for(String s : o.getKeys()) {
				Collection<GetOperation.Callback> cbs=callbacks.get(s);
				if(cbs == null) {
					cbs=new ArrayList<GetOperation.Callback>();
					callbacks.put(s, cbs);
				}
				cbs.add(c);
			}
		}

		public void gotData(String key, int flags, byte[] data) {
			Collection<GetOperation.Callback> cbs=callbacks.get(key);
			assert cbs != null : "No callbacks for key " + key;
			for(GetOperation.Callback c : cbs) {
				c.gotData(key, flags, data);
			}
		}

		public void receivedStatus(OperationStatus status) {
			for(GetOperation.Callback c : allCallbacks) {
				c.receivedStatus(status);
			}
		}

		public void complete() {
			for(GetOperation.Callback c : allCallbacks) {
				c.complete();
			}
		}

		public int numKeys() {
			return callbacks.size();
		}

		public int numCallbacks() {
			return allCallbacks.size();
		}
	}

}
