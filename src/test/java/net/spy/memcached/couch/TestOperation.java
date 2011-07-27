package net.spy.memcached.couch;

import net.spy.memcached.ops.OperationCallback;

public interface TestOperation {
	interface TestCallback extends OperationCallback {
		void getData(String response);
	}
}
