package net.spy.memcached.couch;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import net.spy.memcached.compat.SpyObject;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.params.HttpParams;

public class AsyncConnectionManager extends SpyObject {

	private final HttpHost target;
	private final int maxConnections;
	private final NHttpClientHandler handler;
	private final HttpParams params;
	private final ConnectingIOReactor ioreactor;
	private final Object lock;
	private final Set<NHttpClientConnection> allConns;
	private final Queue<NHttpClientConnection> availableConns;
	private final Queue<AsyncConnectionRequest> pendingRequests;

	private volatile boolean shutdown;

	public AsyncConnectionManager(HttpHost target, int maxConnections,
			NHttpClientHandler handler, HttpParams params)
			throws IOReactorException {
		super();
		this.target = target;
		this.maxConnections = maxConnections;
		this.handler = handler;
		this.params = params;
		this.lock = new Object();
		this.allConns = new HashSet<NHttpClientConnection>();
		this.availableConns = new LinkedList<NHttpClientConnection>();
		this.pendingRequests = new LinkedList<AsyncConnectionRequest>();
		this.ioreactor = new DefaultConnectingIOReactor(2, params);
	}

	public void execute() throws IOException {
		IOEventDispatch dispatch = new DefaultClientIOEventDispatch(
				new ManagedClientHandler(this.handler, this), this.params);
		this.ioreactor.execute(dispatch);
	}

	public void shutdown(long waitMs) throws IOException {
		synchronized (this.lock) {
			if (!this.shutdown) {
				this.shutdown = true;
				while (!this.pendingRequests.isEmpty()) {
					AsyncConnectionRequest request = this.pendingRequests
							.remove();
					request.cancel();
				}
				this.availableConns.clear();
				this.allConns.clear();
			}
		}
		this.ioreactor.shutdown(waitMs);
	}

	void addConnection(NHttpClientConnection conn) {
		if (conn == null) {
			return;
		}
		if (this.shutdown) {
			return;
		}
		synchronized (this.lock) {
			this.allConns.add(conn);
		}
	}

	void removeConnection(NHttpClientConnection conn) {
		if (conn == null) {
			return;
		}
		if (this.shutdown) {
			return;
		}
		synchronized (this.lock) {
			if (this.allConns.remove(conn)) {
				this.availableConns.remove(conn);
			}
			processConnectionRequests();
		}
	}

	public AsyncConnectionRequest requestConnection() {
		if (this.shutdown) {
			throw new IllegalStateException(
					"Connection manager has been shut down");
		}
		AsyncConnectionRequest request = new AsyncConnectionRequest();
		synchronized (this.lock) {
			while (!this.availableConns.isEmpty()) {
				NHttpClientConnection conn = this.availableConns.remove();
				if (conn.isOpen()) {
					getLogger().debug("Re-using persistent connection");
					request.setConnection(conn);
					break;
				} else {
					this.allConns.remove(conn);
				}
			}
			if (!request.isCompleted()) {
				this.pendingRequests.add(request);
				processConnectionRequests();
			}
		}
		return request;
	}

	public void releaseConnection(NHttpClientConnection conn) {
		if (conn == null) {
			return;
		}
		if (this.shutdown) {
			return;
		}
		synchronized (this.lock) {
			if (this.allConns.contains(conn)) {
				if (conn.isOpen()) {
					conn.setSocketTimeout(0);
					AsyncConnectionRequest request = this.pendingRequests
							.poll();
					if (request != null) {
						getLogger().debug("Re-using persistent connection");
						request.setConnection(conn);
					} else {
						this.availableConns.add(conn);
					}
				} else {
					this.allConns.remove(conn);
					processConnectionRequests();
				}
			}
		}
	}

	private void processConnectionRequests() {
		while (this.allConns.size() < this.maxConnections) {
			AsyncConnectionRequest request = this.pendingRequests.poll();
			if (request == null) {
				break;
			}
			InetSocketAddress address = new InetSocketAddress(
					this.target.getHostName(), this.target.getPort());
			ConnRequestCallback callback = new ConnRequestCallback(request);
			getLogger().info("Opening new CouchDB connection");
			this.ioreactor.connect(address, null, request, callback);
		}
	}

	static class ManagedClientHandler implements NHttpClientHandler {

		private final NHttpClientHandler handler;
		private final AsyncConnectionManager connMgr;

		public ManagedClientHandler(NHttpClientHandler handler,
				AsyncConnectionManager connMgr) {
			super();
			this.handler = handler;
			this.connMgr = connMgr;
		}

		public void connected(NHttpClientConnection conn, Object attachment) {
			AsyncConnectionRequest request = (AsyncConnectionRequest) attachment;
			this.handler.connected(conn, attachment);
			this.connMgr.addConnection(conn);
			request.setConnection(conn);
		}

		public void closed(NHttpClientConnection conn) {
			this.connMgr.removeConnection(conn);
			this.handler.closed(conn);
		}

		public void requestReady(NHttpClientConnection conn) {
			this.handler.requestReady(conn);
		}

		public void outputReady(NHttpClientConnection conn,
				ContentEncoder encoder) {
			this.handler.outputReady(conn, encoder);
		}

		public void responseReceived(NHttpClientConnection conn) {
			this.handler.responseReceived(conn);
		}

		public void inputReady(NHttpClientConnection conn,
				ContentDecoder decoder) {
			this.handler.inputReady(conn, decoder);
		}

		public void exception(NHttpClientConnection conn, HttpException ex) {
			this.handler.exception(conn, ex);
		}

		public void exception(NHttpClientConnection conn, IOException ex) {
			this.handler.exception(conn, ex);
		}

		public void timeout(NHttpClientConnection conn) {
			this.handler.timeout(conn);
		}
	}

	static class ConnRequestCallback extends SpyObject implements
			SessionRequestCallback {

		private final AsyncConnectionRequest request;

		public ConnRequestCallback(AsyncConnectionRequest request) {
			super();
			this.request = request;
		}

		public void completed(SessionRequest request) {
			getLogger().info(
					request.getRemoteAddress()
							+ " - Session request successful");
		}

		public void cancelled(SessionRequest request) {
			getLogger()
					.info(request.getRemoteAddress()
							+ " - Session request cancelled");
			this.request.cancel();
		}

		public void failed(SessionRequest request) {
			getLogger().error(
					request.getRemoteAddress() + " - Session request failed");
			IOException ex = request.getException();
			if (ex != null) {
				ex.printStackTrace();
			}
			this.request.cancel();
		}

		public void timeout(SessionRequest request) {
			getLogger()
					.info(request.getRemoteAddress()
							+ " - Session request timed out");
			this.request.cancel();
		}
	}

}
