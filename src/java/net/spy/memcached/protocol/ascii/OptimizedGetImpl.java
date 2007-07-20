package net.spy.memcached.protocol.ascii;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.spy.memcached.ops.GetOperation;

/**
 * Optimized Get operation for folding a bunch of gets together.
 */
class OptimizedGetImpl extends GetOperationImpl
	implements GetOperation.Callback {

	private final Map<String, Collection<GetOperation.Callback>> callbacks=
		new HashMap<String, Collection<GetOperation.Callback>>();
	private final Collection<GetOperation.Callback> allCallbacks=new ArrayList<GetOperation.Callback>();

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

	public void receivedStatus(String line) {
		for(GetOperation.Callback c : allCallbacks) {
			c.receivedStatus(line);
		}
	}


	public void complete() {
		for(GetOperation.Callback c : allCallbacks) {
			c.complete();
		}
	}

	// Wrap a get callback to allow an operation that got rolled up into a
	// multi-operation to return before the entire get operation completes.
	private static class GetCallbackWrapper implements GetOperation.Callback {

		private boolean completed=false;
		private int remainingKeys=0;
		private GetOperation.Callback cb=null;

		public GetCallbackWrapper(int k, GetOperation.Callback c) {
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
			}
		}

		public void complete() {
			assert !completed;
			cb.complete();
			completed=true;
		}
		
	}
}
