/**
 * Copyright (C) 2009-2011 Couchbase, Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */

package net.spy.memcached;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.internal.HttpFuture;
import net.spy.memcached.internal.ViewFuture;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.protocol.couch.DocsOperationImpl;
import net.spy.memcached.protocol.couch.HttpOperation;
import net.spy.memcached.protocol.couch.NoDocsOperationImpl;
import net.spy.memcached.protocol.couch.Paginator;
import net.spy.memcached.protocol.couch.Query;
import net.spy.memcached.protocol.couch.ReducedOperationImpl;
import net.spy.memcached.protocol.couch.View;
import net.spy.memcached.protocol.couch.ViewFetcherOperation;
import net.spy.memcached.protocol.couch.ViewFetcherOperationImpl;
import net.spy.memcached.protocol.couch.ViewOperation.ViewCallback;
import net.spy.memcached.protocol.couch.ViewResponse;
import net.spy.memcached.protocol.couch.ViewRow;
import net.spy.memcached.protocol.couch.ViewsFetcherOperation;
import net.spy.memcached.protocol.couch.ViewsFetcherOperationImpl;
import net.spy.memcached.vbucket.config.Bucket;

import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;

/**
 * A client for Couchbase Server.
 */
public class CouchbaseClient extends MembaseClient
  implements CouchbaseClientIF {

  private static final String MODE_PRODUCTION = "production";
  private static final String MODE_DEVELOPMENT = "development";
  private static final String DEV_PREFIX = "dev_";
  private static final String PROD_PREFIX = "";
  public static final String MODE_PREFIX;
  private static final String MODE_ERROR;

  private CouchbaseConnection cconn;
  private final String bucketName;

  /**
   * Properties priority from highest to lowest:
   *
   * 1. Property defined in user code.
   * 2. Property defined on command line.
   * 3. Property defined in cbclient.properties.
   */
  static {
    Properties properties = new Properties(System.getProperties());
    String viewmode = properties.getProperty("viewmode", null);

    if (viewmode == null) {
      try {
        URL url =  ClassLoader.getSystemResource("cbclient.properties");
        if (url != null) {
          properties.load(new FileInputStream(new File(url.getFile())));
        }
        viewmode = properties.getProperty("viewmode");
      } catch (IOException e) {
        // Properties file doesn't exist. Error logged later.
      }
    }

    if (viewmode == null) {
      MODE_ERROR = "viewmode property isn't defined. Setting viewmode to"
        + " production mode";
      MODE_PREFIX = PROD_PREFIX;
    } else if (viewmode.equals(MODE_PRODUCTION)) {
      MODE_ERROR = "viewmode set to production mode";
      MODE_PREFIX = PROD_PREFIX;
    } else if (viewmode.equals(MODE_DEVELOPMENT)) {
      MODE_ERROR = "viewmode set to development mode";
      MODE_PREFIX = DEV_PREFIX;
    } else {
      MODE_ERROR = "unknown value \"" + viewmode + "\" for property viewmode"
          + " Setting to production mode";
      MODE_PREFIX = PROD_PREFIX;
    }
  }

  public CouchbaseClient(List<URI> baseList, String bucketName, String pwd)
    throws IOException {
    this(baseList, bucketName, bucketName, pwd);
  }

  public CouchbaseClient(List<URI> baseList, String bucketName, String usr,
      String pwd) throws IOException {
    super(new CouchbaseConnectionFactory(baseList, bucketName, usr, pwd),
        false);
    this.bucketName = bucketName;
    CouchbaseConnectionFactory cf = (CouchbaseConnectionFactory) connFactory;
    List<InetSocketAddress> addrs =
        AddrUtil.getAddresses(cf.getVBucketConfig().getServers());

    List<InetSocketAddress> conv = new LinkedList<InetSocketAddress>();
    while (!addrs.isEmpty()) {
      conv.add(addrs.remove(0));
    }
    while (!conv.isEmpty()) {
      addrs.add(new InetSocketAddress(conv.remove(0).getHostName(), 5984));
    }

    getLogger().info(MODE_ERROR);
    cconn = cf.createCouchDBConnection(addrs);
    cf.getConfigurationProvider().subscribe(cf.getBucket(), this);
  }

  /**
   * Gets a view contained in a design document from the cluster.
   *
   * @param designDocumentName the name of the design document.
   * @param viewName the name of the view to get.
   * @return a View object from the cluster.
   * @throws InterruptedException if the operation is interrupted while in
   *           flight
   * @throws ExecutionException if an error occurs during execution
   */
  public HttpFuture<View> asyncGetView(String designDocumentName,
      final String viewName) {
    designDocumentName = MODE_PREFIX + designDocumentName;
    String uri = "/" + bucketName + "/_design/" + designDocumentName;
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<View> crv = new HttpFuture<View>(couchLatch, 60000);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op =
        new ViewFetcherOperationImpl(request, bucketName, designDocumentName,
            viewName, new ViewFetcherOperation.ViewFetcherCallback() {
              private View view = null;

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
   * Gets a future with a list of views for a given design document from the
   * cluster.
   *
   * @param designDocumentName the name of the design document.
   * @return a future containing a List of View objects from the cluster.
   */
  public HttpFuture<List<View>> asyncGetViews(String designDocumentName) {
    designDocumentName = MODE_PREFIX + designDocumentName;
    String uri = "/" + bucketName + "/_design/" + designDocumentName;
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<List<View>> crv =
        new HttpFuture<List<View>>(couchLatch, 60000);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op = new ViewsFetcherOperationImpl(request, bucketName,
        designDocumentName, new ViewsFetcherOperation.ViewsFetcherCallback() {
          private List<View> views = null;

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
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted getting views", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed getting views", e);
    }
  }

  /**
   * Gets a list of views for a given design document from the cluster.
   *
   * @param designDocumentName the name of the design document.
   * @return a list of View objects from the cluster.
   */
  public List<View> getViews(final String designDocumentName) {
    try {
      return asyncGetViews(designDocumentName).get();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted getting views", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed getting views", e);
    }
  }

  public HttpFuture<ViewResponse> asyncQuery(View view, Query query) {
    if (query.willReduce()) {
      return asyncQueryAndReduce(view, query);
    } else if (query.willIncludeDocs()) {
      return asyncQueryAndIncludeDocs(view, query);
    } else {
      return asyncQueryAndExcludeDocs(view, query);
    }
  }

  /**
   * Asynchronously queries a Couchbase view by calling its map function. This
   * type of query will return the view result along with all of the documents
   * for each row in the query.
   *
   * @param view the view to run the query against.
   * @param query the type of query to run against the view.
   * @return a Future containing the results of the query.
   */
  private HttpFuture<ViewResponse> asyncQueryAndIncludeDocs(View view,
      Query query) {
    String uri = view.getURI() + query.toString();
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final ViewFuture crv = new ViewFuture(couchLatch, 60000);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op = new DocsOperationImpl(request, new ViewCallback() {
      private ViewResponse vr = null;

      @Override
      public void receivedStatus(OperationStatus status) {
        if (vr != null) {
          Collection<String> ids = new LinkedList<String>();
          Iterator<ViewRow> itr = vr.iterator();
          while (itr.hasNext()) {
            ids.add(itr.next().getId());
          }
          crv.set(vr, asyncGetBulk(ids), status);
        } else {
          crv.set(null, null, status);
        }
      }

      @Override
      public void complete() {
        couchLatch.countDown();
      }

      @Override
      public void gotData(ViewResponse response) {
        vr = response;
      }
    });
    crv.setOperation(op);
    addOp(op);
    return crv;
  }

  /**
   * Asynchronously queries a Couchbase view by calling its map function. This
   * type of query will return the view result but will not get the documents
   * associated with each row of the query.
   *
   * @param view the view to run the query against.
   * @param query the type of query to run against the view.
   * @return a Future containing the results of the query.
   */
  private HttpFuture<ViewResponse> asyncQueryAndExcludeDocs(View view,
      Query query) {
    String uri = view.getURI() + query.toString();
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<ViewResponse> crv =
        new HttpFuture<ViewResponse>(couchLatch, 60000);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op =
        new NoDocsOperationImpl(request, new ViewCallback() {
          private ViewResponse vr = null;

          @Override
          public void receivedStatus(OperationStatus status) {
            crv.set(vr, status);
          }

          @Override
          public void complete() {
            couchLatch.countDown();
          }

          @Override
          public void gotData(ViewResponse response) {
            vr = response;
          }
        });
    crv.setOperation(op);
    addOp(op);
    return crv;
  }

  /**
   * Asynchronously queries a Couchbase view by calling its map function and
   * then the views reduce function.
   *
   * @param view the view to run the query against.
   * @param query the type of query to run against the view.
   * @return a Future containing the results of the query.
   */
  private HttpFuture<ViewResponse> asyncQueryAndReduce(final View view,
      final Query query) {
    if (!view.hasReduce()) {
      throw new RuntimeException("This view doesn't contain a reduce function");
    }
    String uri = view.getURI() + query.toString();
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<ViewResponse> crv =
        new HttpFuture<ViewResponse>(couchLatch, 60000);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op =
        new ReducedOperationImpl(request, new ViewCallback() {
          private ViewResponse vr = null;

          @Override
          public void receivedStatus(OperationStatus status) {
            crv.set(vr, status);
          }

          @Override
          public void complete() {
            couchLatch.countDown();
          }

          @Override
          public void gotData(ViewResponse response) {
            vr = response;
          }
        });
    crv.setOperation(op);
    addOp(op);
    return crv;
  }

  /**
   * Queries a Couchbase view by calling its map function. This type of query
   * will return the view result along with all of the documents for each row in
   * the query.
   *
   * @param view the view to run the query against.
   * @param query the type of query to run against the view.
   * @return a ViewResponseWithDocs containing the results of the query.
   */
  public ViewResponse query(View view, Query query) {
    try {
      return asyncQuery(view, query).get();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while accessing the view", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed to access the view", e);
    }
  }

  /**
   * A paginated query allows the user to get the results of a large query in
   * small chunks allowing for better performance. The result allows you
   * to iterate through the results of the query and when you get to the end
   * of the current result set the client will automatically fetch the next set
   * of results.
   *
   * @param view the view to query against.
   * @param query the query for this request.
   * @param docsPerPage the amount of documents per page.
   * @return A Paginator (iterator) to use for reading the results of the query.
   */
  public Paginator paginatedQuery(View view, Query query, int docsPerPage) {
    return new Paginator(this, view, query, 10);
  }

  /**
   * Adds an operation to the queue where it waits to be sent to Couchbase. This
   * function is for internal use only.
   */
  public void addOp(final HttpOperation op) {
    cconn.checkState();
    cconn.addOp(op);
  }

  /**
   * This function is called when there is a topology change in the cluster.
   * This function is intended for internal use only.
   */
  @Override
  public void reconfigure(Bucket bucket) {
    reconfiguring = true;
    try {
      mconn.reconfigure(bucket);
      cconn.reconfigure(bucket);
    } catch (IllegalArgumentException ex) {
      getLogger().warn(
          "Failed to reconfigure client, staying with previous configuration.",
          ex);
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
