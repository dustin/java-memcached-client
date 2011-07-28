package net.spy.memcached;

import java.util.List;

import net.spy.memcached.internal.HttpFuture;
import net.spy.memcached.internal.ViewFuture;
import net.spy.memcached.protocol.couch.ViewResponseNoDocs;
import net.spy.memcached.protocol.couch.Query;
import net.spy.memcached.protocol.couch.ViewResponseReduced;
import net.spy.memcached.protocol.couch.View;

public interface CouchbaseClientIF {

	// View Access
	public HttpFuture<View> asyncGetView(final String designDocumentName, final String viewName);

	public HttpFuture<List<View>> asyncGetViews(final String designDocumentName);

	// Query
	public ViewFuture query(View view, Query query);

	public HttpFuture<ViewResponseNoDocs> queryAndExcludeDocs(View view, Query query);

	public HttpFuture<ViewResponseReduced> queryAndReduce(View view, Query query);
}
