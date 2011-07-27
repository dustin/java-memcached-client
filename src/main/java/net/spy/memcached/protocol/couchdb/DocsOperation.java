package net.spy.memcached.protocol.couchdb;

import net.spy.memcached.ops.OperationCallback;

public interface DocsOperation {
	interface DocsCallback extends OperationCallback {
		void gotData(ViewResponseWithDocs response);
	}
}
