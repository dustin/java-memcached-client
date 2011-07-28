package net.spy.memcached.protocol.couch;

import net.spy.memcached.ops.OperationCallback;

public interface NoDocsOperation {

	interface NoDocsCallback extends OperationCallback {
		void gotData(ViewResponseNoDocs response);
	}
}
