package net.spy.memcached.protocol.couch;

import net.spy.memcached.ops.OperationCallback;

public interface ReducedOperation {

	interface ReducedCallback extends OperationCallback {
		void gotData(ViewResponseReduced response);
	}

}
