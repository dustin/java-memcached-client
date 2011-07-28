package net.spy.memcached.protocol.couch;

import java.util.List;

import net.spy.memcached.ops.OperationCallback;

public interface ViewsOperation {
	interface ViewsCallback extends OperationCallback {
		void gotData(List<View> views);
	}
}
