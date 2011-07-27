package net.spy.memcached.protocol.couchdb;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

public interface HttpOperation {

	public HttpRequest getRequest();

	OperationCallback getCallback();

	boolean isCancelled();

	boolean hasErrored();

	boolean isTimedOut();

	void cancel();

	void timeOut();

	OperationException getException();

	void handleResponse(HttpResponse response);
}
