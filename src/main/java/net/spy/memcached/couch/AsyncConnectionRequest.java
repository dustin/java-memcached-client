package net.spy.memcached.couch;

import org.apache.http.nio.NHttpClientConnection;

public class AsyncConnectionRequest {

	private volatile boolean completed;
	private volatile NHttpClientConnection conn;

	public AsyncConnectionRequest() {
		super();
	}

	public boolean isCompleted() {
		return this.completed;
	}

	public void setConnection(NHttpClientConnection conn) {
		if (this.completed) {
			return;
		}
		this.completed = true;
		synchronized (this) {
			this.conn = conn;
			notifyAll();
		}
	}

	public NHttpClientConnection getConnection() {
		return this.conn;
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
