package net.spy.memcached.protocol.couchdb;

import net.spy.memcached.ops.OperationException;

import org.apache.http.HttpRequest;

public class HttpOperation {

	private final HttpRequest request;
	private final HttpCallback callback;
	private OperationException exception;
	private boolean cancelled;
	private boolean errored;
	private boolean timedOut;

	public HttpOperation(HttpRequest r, HttpCallback cb) {
		request = r;
		callback = cb;
		exception = null;
		cancelled = false;
		errored = false;
		timedOut = false;
	}

	public HttpRequest getRequest() {
		return request;
	}

	public HttpCallback getCallback() {
		return callback;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public boolean hasErrored() {
		return errored;
	}

	public boolean isTimedOut() {
		return timedOut;
	}

	public void cancel() {
		cancelled = true;
	}

	public void timeOut() {
		timedOut = true;
	}

	public OperationException getException() {
		return exception;
	}

	public void setException(OperationException e) {
		exception = e;
		errored = true;
	}
}
