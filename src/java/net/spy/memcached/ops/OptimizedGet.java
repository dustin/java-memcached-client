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
		Callback c=new GetCallbackWrapper(o.getKeys().size(), o.getCallback());
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

	// Wrap a get callback to allow an operation that got rolled up into a
	// multi-operation to return before the entire get operation completes.
	private static class GetCallbackWrapper implements Callback {

		private boolean completed=false;
		private int remainingKeys=0;
		private Callback cb=null;

		public GetCallbackWrapper(int k, Callback c) {
			super();
			remainingKeys=k;
			cb=c;
		}

		public void gotData(String key, int flags, byte[] data) {
			assert !completed : "Got data for a completed wrapped op";
			cb.gotData(key, flags, data);
			if(--remainingKeys == 0) {
				// Fake a status line
				receivedStatus("END");
			}
		}

		public void receivedStatus(String line) {
			if(!completed) {
				cb.receivedStatus(line);
				completed=true;
			}
		}
		
	}
}
