package net.spy.memcached.protocol.ascii;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.protocol.GetCallbackWrapper;

/**
 * Optimized Get operation for folding a bunch of gets together.
 */
final class OptimizedGetImpl extends GetOperationImpl
	implements GetOperation.Callback {

	private final Map<String, Collection<GetOperation.Callback>> callbacks=
		new HashMap<String, Collection<GetOperation.Callback>>();
	private final Collection<GetOperation.Callback> allCallbacks=
		new ArrayList<GetOperation.Callback>();

	/**
	 * Construct an optimized get starting with the given get operation.
	 */
	public OptimizedGetImpl(GetOperation firstGet) {
		super();
		setKeys(callbacks.keySet());
		setCallback(this);
		addOperation(firstGet);
	}

	/**
	 * Add a new GetOperation to get.
	 */
	public void addOperation(GetOperation o) {
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

	public int numKeys() {
		return callbacks.size();
	}

	public int numCallbacks() {
		return allCallbacks.size();
	}

	//
	// This is its callback implementation
	//

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
}
