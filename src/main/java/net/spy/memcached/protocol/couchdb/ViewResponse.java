package net.spy.memcached.protocol.couchdb;


public interface ViewResponse<T> extends Iterable<T> {
	int size();
}
