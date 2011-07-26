package net.spy.memcached.couch;

import org.apache.http.nio.NHttpClientConnection;

public class RequestHandle {

	private final AsyncConnectionManager connMgr;
	private final NHttpClientConnection conn;

	private volatile boolean completed;

	public RequestHandle(AsyncConnectionManager connMgr,
			NHttpClientConnection conn) {
		super();
		this.connMgr = connMgr;
		this.conn = conn;
	}

	public boolean isCompleted() {
		return this.completed;
	}

	public void completed() {
		if (this.completed) {
			return;
		}
		this.completed = true;
		this.connMgr.releaseConnection(this.conn);
		synchronized (this) {
			notifyAll();
		}
	}

	public void cancel() {
		if (this.completed) {
			return;
		}
		this.completed = true;
		synchronized (this) {
			notifyAll();
		}
	}

	public void waitFor() throws InterruptedException {
		if (this.completed) {
			return;
		}
		synchronized (this) {
			while (!this.completed) {
				wait();
			}
		}
	}
}