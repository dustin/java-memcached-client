package net.spy.memcached.protocol.couch;

import net.spy.memcached.ops.OperationCallback;

public interface DocsOperation {
	interface DocsCallback extends OperationCallback {
		void gotData(ViewResponseWithDocs response);
	}
}
