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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
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
import net.spy.memcached.protocol.couch.DocsOperation.DocsCallback;
import net.spy.memcached.protocol.couch.DocsOperationImpl;
import net.spy.memcached.protocol.couch.HttpOperation;
import net.spy.memcached.protocol.couch.NoDocsOperation;
import net.spy.memcached.protocol.couch.NoDocsOperationImpl;
import net.spy.memcached.protocol.couch.Query;
import net.spy.memcached.protocol.couch.ReducedOperation.ReducedCallback;
import net.spy.memcached.protocol.couch.ReducedOperationImpl;
import net.spy.memcached.protocol.couch.RowWithDocs;
import net.spy.memcached.protocol.couch.View;
import net.spy.memcached.protocol.couch.ViewOperation.ViewCallback;
import net.spy.memcached.protocol.couch.ViewOperationImpl;
import net.spy.memcached.protocol.couch.ViewResponseNoDocs;
import net.spy.memcached.protocol.couch.ViewResponseReduced;
import net.spy.memcached.protocol.couch.ViewResponseWithDocs;
import net.spy.memcached.protocol.couch.ViewsOperation.ViewsCallback;
import net.spy.memcached.protocol.couch.ViewsOperationImpl;
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

  static {
    String viewmode = null;
    boolean propsFileExists;
    try {
      Properties properties = new Properties();
      properties.load(new FileInputStream("config.properties"));
      viewmode = properties.getProperty("viewmode");
      propsFileExists = true;
    } catch (IOException e) {
      propsFileExists = false;
    }
    if (!propsFileExists) {
      MODE_ERROR = "Can't find config.properties. Setting viewmode "
          + "to production mode";
      MODE_PREFIX = PROD_PREFIX;
    } else if (viewmode == null) {
      MODE_ERROR = "viewmode doesn't exist in config.properties. "
              + "Setting viewmode to production mode";
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
        new ViewOperationImpl(request, bucketName, designDocumentName,
            viewName, new ViewCallback() {
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
    final HttpOperation op = new ViewsOperationImpl(request, bucketName,
        designDocumentName, new ViewsCallback() {
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

  /**
   * Asynchronously queries a Couchbase view by calling its map function. This
   * type of query will return the view result along with all of the documents
   * for each row in the query.
   *
   * @param view the view to run the query against.
   * @param query the type of query to run against the view.
   * @return a Future containing the results of the query.
   */
  public ViewFuture asyncQuery(View view, Query query) {
    String queryString = query.toString();
    String params = (queryString.length() > 0) ? "&reduce=false"
        : "?reduce=false";

    String uri = view.getURI() + queryString + params;
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final ViewFuture crv = new ViewFuture(couchLatch, 60000);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op = new DocsOperationImpl(request, new DocsCallback() {
      private ViewResponseWithDocs vr = null;

      @Override
      public void receivedStatus(OperationStatus status) {
        if (vr != null) {
          Collection<String> ids = new LinkedList<String>();
          Iterator<RowWithDocs> itr = vr.iterator();
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
      public void gotData(ViewResponseWithDocs response) {
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
  public HttpFuture<ViewResponseNoDocs> asyncQueryAndExcludeDocs(View view,
      Query query) {
    String queryString = query.toString();
    String params = (queryString.length() > 0) ? "&reduce=false"
        : "?reduce=false";
    params += "&include_docs=false";

    String uri = view.getURI() + queryString + params;
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<ViewResponseNoDocs> crv =
        new HttpFuture<ViewResponseNoDocs>(couchLatch, 60000);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op =
        new NoDocsOperationImpl(request, new NoDocsOperation.NoDocsCallback() {
          private ViewResponseNoDocs vr = null;

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
   * Asynchronously queries a Couchbase view by calling its map function and
   * then the views reduce function.
   *
   * @param view the view to run the query against.
   * @param query the type of query to run against the view.
   * @return a Future containing the results of the query.
   */
  public HttpFuture<ViewResponseReduced> asyncQueryAndReduce(final View view,
      final Query query) {
    if (!view.hasReduce()) {
      throw new RuntimeException("This view doesn't contain a reduce function");
    }
    String uri = view.getURI() + query.toString();
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<ViewResponseReduced> crv =
        new HttpFuture<ViewResponseReduced>(couchLatch, 60000);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op =
        new ReducedOperationImpl(request, new ReducedCallback() {
          private ViewResponseReduced vr = null;

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
   * Queries a Couchbase view by calling its map function. This type of query
   * will return the view result along with all of the documents for each row in
   * the query.
   *
   * @param view the view to run the query against.
   * @param query the type of query to run against the view.
   * @return a ViewResponseWithDocs containing the results of the query.
   */
  public ViewResponseWithDocs query(View view, Query query) {
    try {
      return asyncQuery(view, query).get();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while accessing the view", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed to access the view", e);
    }
  }

  /**
   * Queries a Couchbase view by calling its map function. This type of query
   * will return the view result but will not get the documents associated with
   * each row of the query.
   *
   * @param view the view to run the query against.
   * @param query the type of query to run against the view.
   * @return a ViewResponseNoDocs containing the results of the query.
   */
  public ViewResponseNoDocs queryAndExcludeDocs(View view, Query query) {
    try {
      return asyncQueryAndExcludeDocs(view, query).get();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while accessing the view", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed to access the view", e);
    }
  }

  /**
   * Queries a Couchbase view by calling its map function and then the views
   * reduce function.
   *
   * @param view the view to run the query against.
   * @param query the type of query to run against the view.
   * @return a Future containing the results of the query.
   */
  public ViewResponseReduced queryAndReduce(View view, Query query) {
    try {
      return asyncQueryAndReduce(view, query).get();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while accessing the view", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed to access the view", e);
    }
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
