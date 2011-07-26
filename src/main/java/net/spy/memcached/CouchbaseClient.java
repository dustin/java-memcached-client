package net.spy.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;

import net.spy.memcached.internal.HttpFuture;
import net.spy.memcached.protocol.couchdb.DocParserUtils;
import net.spy.memcached.protocol.couchdb.HttpCallback;
import net.spy.memcached.protocol.couchdb.HttpOperation;
import net.spy.memcached.protocol.couchdb.View;
import net.spy.memcached.vbucket.ConfigurationException;

public class CouchbaseClient extends MembaseClient {

	private CouchbaseConnection cconn;
	private final String bucketName;

	public CouchbaseClient(List<URI> baseList, String bucketName, String pwd)
			throws IOException, ConfigurationException {
		this(baseList, bucketName, bucketName, pwd);
	}

	public CouchbaseClient(List<URI> baseList, String bucketName, String usr, String pwd)
			throws IOException, ConfigurationException {
		super(new CouchbaseConnectionFactory(baseList, bucketName, usr, pwd));
		this.bucketName = bucketName;
		CouchbaseConnectionFactory cf = (CouchbaseConnectionFactory)connFactory;
		List<InetSocketAddress> addrs = AddrUtil.getAddresses(cf.getVBucketConfig().getServers());

		List<InetSocketAddress> conv = new LinkedList<InetSocketAddress>();
		while (!addrs.isEmpty()) { conv.add(addrs.remove(0)); }
		while (!conv.isEmpty()) { addrs.add(new InetSocketAddress(conv.remove(0).getHostName(), 5984)); }

		cconn = cf.createCouchDBConnection(addrs);
	}

	/**
	 * Gets a view contained in a design document from the cluster.
	 *
	 * @param designDocumentName the name of the design document.
	 * @param viewName the name of the view to get.
	 * @return a View object from the cluster.
	 * @throws InterruptedException if the operation is interrupted while in flight
	 * @throws ExecutionException if an error occurs during execution
	 */
	public HttpFuture<View> asyncGetView(final String designDocumentName, final String viewName) {
		String uri = "/" + bucketName + "/_design/" + designDocumentName;
		final CountDownLatch couchLatch = new CountDownLatch(1);
		final HttpFuture<View> crv =
			new HttpFuture<View>(couchLatch, operationTimeout);

		final HttpRequest request = new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
		final HttpOperation op = new HttpOperation(request, new HttpCallback() {
			@Override
			public void complete(String response) {

				Collection<View> views;
				try {
					views = DocParserUtils.parseDesignDocumentForViews(bucketName, designDocumentName, response);
					crv.set(null);
					for (View v : views) {
						if (v.getViewName().equals(viewName)) {
							crv.set(v);
							break;
						}
					}
				} catch (ParseException e) {
					crv.set(null);
					e.printStackTrace();
				}
				couchLatch.countDown();
			}
		});
		crv.setOperation(op);
		addOp(op);
		return crv;
	}

	/**
	 * Gets a list of views for a given design document from the cluster.
	 *
	 * @param designDocumentName the name of the design document.
	 * @param viewName the name of the view to get.
	 * @return a View object from the cluster.
	 * @throws InterruptedException if the operation is interrupted while in flight
	 * @throws ExecutionException if an error occurs during execution
	 */

	public HttpFuture<List<View>> asyncGetViews(final String designDocumentName) {
		String uri = "/" + bucketName + "/_design/" + designDocumentName;
		final CountDownLatch couchLatch = new CountDownLatch(1);
		final HttpFuture<List<View>> crv =
			new HttpFuture<List<View>>(couchLatch, operationTimeout);

		final HttpRequest request = new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
		final HttpOperation op = new HttpOperation(request, new HttpCallback() {
			@Override
			public void complete(String response) {
				try {
					crv.set(DocParserUtils.parseDesignDocumentForViews(bucketName, designDocumentName, response));
				} catch (ParseException e) {
					getLogger().error(e.getMessage());
				}
				couchLatch.countDown();
			}
		});
		crv.setOperation(op);
		addOp(op);
		return crv;
	}

	/**
	 * Gets a view contained in a design document from the cluster.
	 *
	 * @param designDocumentName the name of the design document.
	 * @param viewName the name of the view to get.
	 * @return a View object from the cluster.
	 */
	public View getView(final String designDocumentName, final String viewName) {
		try {
			return asyncGetView(designDocumentName, viewName).get();
		}catch (InterruptedException e) {
			throw new RuntimeException("Interrupted getting views", e);
		} catch (ExecutionException e) {
			throw new RuntimeException("Failed getting views", e);
		}
	}

	/**
	 * Gets  a list of views for a given design document from the cluster.
	 *
	 * @param designDocumentName the name of the design document.
	 * @param viewName the name of the view to get.
	 * @return a list of View objects from the cluster.
	 */
	public List<View> getViews(final String designDocumentName) {
		try {
			return asyncGetViews(designDocumentName).get();
		}catch (InterruptedException e) {
			throw new RuntimeException("Interrupted getting views", e);
		} catch (ExecutionException e) {
			throw new RuntimeException("Failed getting views", e);
		}
	}

	/**
	 * Adds an operation to the queue where it waits to be sent to
	 * Couchbase. This function is for internal use only.
	 */
	public void addOp(final HttpOperation op) {
		cconn.checkState();
		cconn.addOp(op);
	}

	/**
	 * Shuts down the client.
	 */
	@Override
	public void shutdown() {
		shutdown(-1, TimeUnit.MILLISECONDS);
	}

	@Override
	public boolean shutdown(long duration, TimeUnit units) {
		try {
			return super.shutdown(duration, units) && cconn.shutdown();
		} catch (IOException e) {
			getLogger().error("Error shutting down CouchbaseClient");
			return false;
		}
	}
}
