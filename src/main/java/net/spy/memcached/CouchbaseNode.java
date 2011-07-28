/**
 * Based upon http://hc.apache.org/httpcomponents-core-ga/httpcore-nio/examples/org/apache/http/examples/nio/NHttpClientConnManagement.java
 */

package net.spy.memcached;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.couch.AsyncConnectionManager;
import net.spy.memcached.couch.AsyncConnectionRequest;
import net.spy.memcached.couch.RequestHandle;
import net.spy.memcached.protocol.couchdb.HttpOperation;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.entity.BufferingNHttpEntity;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.protocol.NHttpRequestExecutionHandler;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.protocol.HttpContext;

public class CouchbaseNode extends SpyObject {

	private final InetSocketAddress addr;
	private final AsyncConnectionManager connMgr;

	private final long opQueueMaxBlockTime;
	private final long defaultOpTimeout;

	private final BlockingQueue<HttpOperation> writeQ;

	public CouchbaseNode(InetSocketAddress a, AsyncConnectionManager mgr,
			LinkedBlockingQueue<HttpOperation> linkedBlockingQueue, long maxBlockTime,
			long operationTimeout) {
		addr = a;
		connMgr = mgr;
		writeQ = linkedBlockingQueue;
		opQueueMaxBlockTime = maxBlockTime;
		defaultOpTimeout = operationTimeout;
	}

	public void init() throws IOReactorException {
		// Start the I/O reactor in a separate thread
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					connMgr.execute();
				} catch (InterruptedIOException ex) {
					getLogger().error("I/O reactor Interrupted");
				} catch (IOException e) {
					getLogger().error("I/O error: " + e.getMessage());
					e.printStackTrace();
				}
				getLogger().info("Couchbase I/O reactor terminated");
			}
		});
		t.start();
	}

	public void doWrites() {
		HttpOperation op;
		while ((op = writeQ.poll()) != null) {
			if (!op.isTimedOut() && !op.isCancelled()) {
				AsyncConnectionRequest connRequest = connMgr
						.requestConnection();
				try {
					connRequest.waitFor();
				} catch (InterruptedException e) {
					getLogger().warn(
							"Interrupted while trying to get a connection."
									+ " Cancelling op");
					op.cancel();
					return;
				}

				NHttpClientConnection conn = connRequest.getConnection();
				if (conn == null) {
					getLogger().error("Failed to obtain connection. " +
							"Cancelling op");
					op.cancel();
				} else {
					HttpContext context = conn.getContext();
					RequestHandle handle = new RequestHandle(connMgr, conn);
					context.setAttribute("request-handle", handle);
					context.setAttribute("operation", op);
					conn.requestOutput();
				}
			}
		}
	}

	public Collection<HttpOperation> destroyWriteQueue() {
		Collection<HttpOperation> rv=new ArrayList<HttpOperation>();
		writeQ.drainTo(rv);
		return rv;
	}

	public boolean hasWriteOps() {
		return !writeQ.isEmpty();
	}

	public void addOp(HttpOperation op) {
		try {
			if (!writeQ.offer(op, opQueueMaxBlockTime, TimeUnit.MILLISECONDS)) {
				throw new IllegalStateException("Timed out waiting to add "
						+ op + "(max wait=" + opQueueMaxBlockTime + "ms)");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting to add "
					+ op);
		}
	}

	public InetSocketAddress getSocketAddress() {
		return addr;
	}

	public void shutdown() throws IOException {
		shutdown(0, TimeUnit.MILLISECONDS);
	}

	public void shutdown(long time, TimeUnit unit) throws IOException {
		if (unit != TimeUnit.MILLISECONDS) {
			connMgr.shutdown(TimeUnit.MILLISECONDS.convert(time, unit));
		} else {
			connMgr.shutdown(time);
		}
	}

	static class MyHttpRequestExecutionHandler implements
			NHttpRequestExecutionHandler {

		public MyHttpRequestExecutionHandler() {
			super();
		}

		public void initalizeContext(final HttpContext context,
				final Object attachment) {
		}

		public void finalizeContext(final HttpContext context) {
			RequestHandle handle = (RequestHandle) context
					.removeAttribute("request-handle");
			if (handle != null) {
				handle.cancel();
			}
		}

		public HttpRequest submitRequest(final HttpContext context) {
			HttpOperation op = (HttpOperation) context.getAttribute("operation");
			if (op == null) {
				return null;
			}
			return op.getRequest();
		}

		public void handleResponse(final HttpResponse response,
				final HttpContext context) {
			RequestHandle handle = (RequestHandle) context.removeAttribute("request-handle");
			HttpOperation op = (HttpOperation) context.removeAttribute("operation");
			if (handle != null) {
				handle.completed();
				op.handleResponse(response);
			}
		}

		@Override
		public ConsumingNHttpEntity responseEntity(HttpResponse response,
				HttpContext context) throws IOException {
			return new BufferingNHttpEntity(response.getEntity(),
					new HeapByteBufferAllocator());
		}
	}

	static class EventLogger extends SpyObject implements EventListener {

		public void connectionOpen(final NHttpConnection conn) {
			getLogger().debug("Connection open: " + conn);
		}

		public void connectionTimeout(final NHttpConnection conn) {
			getLogger().error("Connection timed out: " + conn);
		}

		public void connectionClosed(final NHttpConnection conn) {
			getLogger().debug("Connection closed: " + conn);
		}

		public void fatalIOException(final IOException ex,
				final NHttpConnection conn) {
			getLogger().error("I/O error: " + ex.getMessage());
		}

		public void fatalProtocolException(final HttpException ex,
				final NHttpConnection conn) {
			getLogger().error("HTTP error: " + ex.getMessage());
		}

	}

}
