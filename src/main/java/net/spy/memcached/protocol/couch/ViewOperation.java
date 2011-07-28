package net.spy.memcached.protocol.couch;

import net.spy.memcached.ops.OperationCallback;

public interface ViewOperation {
	public interface ViewCallback extends OperationCallback {
		void gotData(View view);
	}
}
