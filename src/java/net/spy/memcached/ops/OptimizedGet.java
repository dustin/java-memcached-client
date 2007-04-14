package net.spy.memcached.ops;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Optimized Get operation for folding a bunch of gets together.
 */
public class OptimizedGet extends GetOperation
	implements GetOperation.Callback {

	private Map<String, Collection<Callback>> callbacks=
		new HashMap<String, Collection<Callback>>();
	private Collection<Callback> allCallbacks=new ArrayList<Callback>();

	/**
	 * Construct an optimized get starting with the given get operation.
	 */
	public OptimizedGet(GetOperation firstGet) {
		super();
		setKeys(callbacks.keySet());
		setCallback(this);
		addOperation(firstGet);
	}

	/**
	 * Add a new GetOperation to get.
	 */
	public void addOperation(GetOperation o) {
		Callback c=o.getCallback();
		allCallbacks.add(c);
		for(String s : o.getKeys()) {
			Collection<Callback> cbs=callbacks.get(s);
			if(cbs == null) {
				cbs=new ArrayList<Callback>();
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
		Collection<Callback> cbs=callbacks.get(key);
		assert cbs != null : "No callbacks for key " + key;
		for(Callback c : cbs) {
			c.gotData(key, flags, data);
		}
	}

	public void receivedStatus(String line) {
		for(Callback c : allCallbacks) {
			c.receivedStatus(line);
		}
	}
}
