package net.spy.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;

import net.spy.memcached.internal.HttpFuture;
import net.spy.memcached.internal.ViewFuture;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.protocol.couchdb.DocsOperation.DocsCallback;
import net.spy.memcached.protocol.couchdb.DocsOperationImpl;
import net.spy.memcached.protocol.couchdb.HttpOperation;
import net.spy.memcached.protocol.couchdb.NoDocsOperation;
import net.spy.memcached.protocol.couchdb.NoDocsOperationImpl;
import net.spy.memcached.protocol.couchdb.Query;
import net.spy.memcached.protocol.couchdb.ReducedOperation.ReducedCallback;
import net.spy.memcached.protocol.couchdb.ReducedOperationImpl;
import net.spy.memcached.protocol.couchdb.RowWithDocs;
import net.spy.memcached.protocol.couchdb.View;
import net.spy.memcached.protocol.couchdb.ViewOperation.ViewCallback;
import net.spy.memcached.protocol.couchdb.ViewOperationImpl;
import net.spy.memcached.protocol.couchdb.ViewsOperation.ViewsCallback;
import net.spy.memcached.protocol.couchdb.ViewsOperationImpl;
import net.spy.memcached.protocol.couchdb.ViewResponseNoDocs;
import net.spy.memcached.protocol.couchdb.ViewResponseReduced;
import net.spy.memcached.protocol.couchdb.ViewResponseWithDocs;



import net.spy.memcached.vbucket.ConfigurationException;
import net.spy.memcached.vbucket.config.Bucket;

public class CouchbaseClient extends MembaseClient implements CouchbaseClientIF {

	private CouchbaseConnection cconn;
	private final String bucketName;

	public CouchbaseClient(List<URI> baseList, String bucketName, String pwd)
			throws IOException, ConfigurationException {
		this(baseList, bucketName, bucketName, pwd);
	}

	public CouchbaseClient(List<URI> baseList, String bucketName, String usr, String pwd)
			throws IOException, ConfigurationException {
		super(new CouchbaseConnectionFactory(baseList, bucketName, usr, pwd), false);
		this.bucketName = bucketName;
		CouchbaseConnectionFactory cf = (CouchbaseConnectionFactory)connFactory;
		List<InetSocketAddress> addrs = AddrUtil.getAddresses(cf.getVBucketConfig().getServers());

		List<InetSocketAddress> conv = new LinkedList<InetSocketAddress>();
		while (!addrs.isEmpty()) { conv.add(addrs.remove(0)); }
		while (!conv.isEmpty()) { addrs.add(new InetSocketAddress(conv.remove(0).getHostName(), 5984)); }

		cconn = cf.createCouchDBConnection(addrs);
		cf.getConfigurationProvider().subscribe(cf.getBucket(), this);
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
		final HttpOperation op = new ViewOperationImpl(request, bucketName,
				designDocumentName, viewName, new ViewCallback() {
			View view = null;
			@Override
			public void receivedStatus(OperationStatus status) {
				crv.set(view, status);
			}
			@Override
			public void complete() {
				couchLatch.countDown();
			}
			@Override
			public void gotData(View v) {
				view = v;
			}
		});
		crv.setOperation(op);
		addOp(op);
		return crv;
	}

	/**
	 * Gets a future with a list of views for a given design document from the cluster.
	 *
	 * @param designDocumentName the name of the design document.
	 * @return a future containing a List of View objects from the cluster.
	 */
	public HttpFuture<List<View>> asyncGetViews(final String designDocumentName) {
		String uri = "/" + bucketName + "/_design/" + designDocumentName;
		final CountDownLatch couchLatch = new CountDownLatch(1);
		final HttpFuture<List<View>> crv =
			new HttpFuture<List<View>>(couchLatch, operationTimeout);

		final HttpRequest request = new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
		final HttpOperation op = new ViewsOperationImpl(request, bucketName,
				designDocumentName, new ViewsCallback() {
			List<View> views = null;
			@Override
			public void receivedStatus(OperationStatus status) {
				crv.set(views, status);
			}
			@Override
			public void complete() {
				couchLatch.countDown();
			}
			@Override
			public void gotData(List<View> v) {
				views = v;
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
	 * Queries a Couchbase view by calling its map function. This type
	 * of query will return the view result along with all of the
	 * documents for each row in the query.
	 *
	 * @param view the view to run the query against.
	 * @param query the type of query to run against the view.
	 * @return a Future containing the results of the query.
	 */
	public ViewFuture query(View view, Query query) {
		String queryString = query.toString();
		String params = (queryString.length() > 0) ? "&reduce=false" : "?reduce=false";

		String uri = view.getURI() + queryString + params;
		final CountDownLatch couchLatch = new CountDownLatch(1);
		final ViewFuture crv = new ViewFuture(couchLatch, operationTimeout);

		final HttpRequest request = new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
		final HttpOperation op = new DocsOperationImpl(request, new DocsCallback() {
			ViewResponseWithDocs vr = null;
			@Override
			public void receivedStatus(OperationStatus status) {
				Collection<String> ids = new LinkedList<String>();
				Iterator<RowWithDocs> itr = vr.iterator();
				while (itr.hasNext()) {
					ids.add(itr.next().getId());
				}
				crv.set(vr, asyncGetBulk(ids), status);
			}
			@Override
			public void complete() {
				couchLatch.countDown();
			}
			@Override
			public void gotData(ViewResponseWithDocs response) {
				vr = response;
			}
		});
		crv.setOperation(op);
		addOp(op);
		return crv;
	}

	/**
	 * Queries a Couchbase view by calling it's map function. This type
	 * of query will return the view result but will not get the
	 * documents associated with each row of the query.
	 *
	 * @param view the view to run the query against.
	 * @param query the type of query to run against the view.
	 * @return a Future containing the results of the query.
	 */
	public HttpFuture<ViewResponseNoDocs> queryAndExcludeDocs(View view, Query query) {
		String queryString = query.toString();
		String params = (queryString.length() > 0) ? "&reduce=false" : "?reduce=false";
		params += "&include_docs=false";

		String uri = view.getURI() + queryString + params;
		final CountDownLatch couchLatch = new CountDownLatch(1);
		final HttpFuture<ViewResponseNoDocs> crv =
			new HttpFuture<ViewResponseNoDocs>(couchLatch, operationTimeout);

		final HttpRequest request = new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
		final HttpOperation op = new NoDocsOperationImpl(request, new NoDocsOperation.NoDocsCallback() {
			ViewResponseNoDocs vr = null;
			@Override
			public void receivedStatus(OperationStatus status) {
				crv.set(vr, status);
			}
			@Override
			public void complete() {
				couchLatch.countDown();
			}
			@Override
			public void gotData(ViewResponseNoDocs response) {
				vr = response;
			}
		});
		crv.setOperation(op);
		addOp(op);
		return crv;
	}

	/**
	 * Queries a Couchbase view by calling it's map function and then
	 * the views reduce function.
	 *
	 * @param view the view to run the query against.
	 * @param query the type of query to run against the view.
	 * @return a Future containing the results of the query.
	 */
	public HttpFuture<ViewResponseReduced> queryAndReduce(final View view, final Query query){
		if (!view.hasReduce()) {
			throw new RuntimeException("This view doesn't contain a reduce function");
		}
		String uri = view.getURI() + query.toString();
		final CountDownLatch couchLatch = new CountDownLatch(1);
		final HttpFuture<ViewResponseReduced> crv =
			new HttpFuture<ViewResponseReduced>(couchLatch, operationTimeout);

		final HttpRequest request = new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
		final HttpOperation op = new ReducedOperationImpl(request, new ReducedCallback() {
			ViewResponseReduced vr = null;
			@Override
			public void receivedStatus(OperationStatus status) {
				crv.set(vr, status);
			}
			@Override
			public void complete() {
				couchLatch.countDown();
			}
			@Override
			public void gotData(ViewResponseReduced response) {
				vr = response;
			}
		});
		crv.setOperation(op);
		addOp(op);
		return crv;
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
	 * This function is called when there is a topology change in the
	 * cluster. This function is intended for internal use only.
	 */
	@Override
	public void reconfigure(Bucket bucket) {
		reconfiguring = true;
		try {
			mconn.reconfigure(bucket);
			cconn.reconfigure(bucket);
		} catch (IllegalArgumentException ex) {
			getLogger().warn("Failed to reconfigure client, staying with previous configuration.", ex);
		} finally {
			reconfiguring = false;
		}
	}

	/**
	 * Shuts down the client immediately.
	 */
	@Override
	public void shutdown() {
		shutdown(-1, TimeUnit.MILLISECONDS);
	}

	/**
	 * Shut down this client gracefully.
	 *
	 * @param duration the amount of time time for shutdown
	 * @param units the TimeUnit for the timeout
	 * @return result of the shutdown request
	 */
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
