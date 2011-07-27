package net.spy.memcached.protocol.couchdb;

import java.util.Collection;

public interface ViewResponse<T> extends Iterable<T> {
	Collection<RowError> getErrors();

	int size();
}
