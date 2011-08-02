package net.spy.memcached;

import java.util.List;

import net.spy.memcached.internal.HttpFuture;
import net.spy.memcached.internal.ViewFuture;
import net.spy.memcached.protocol.couch.ViewResponseNoDocs;
import net.spy.memcached.protocol.couch.Query;
import net.spy.memcached.protocol.couch.ViewResponseReduced;
import net.spy.memcached.protocol.couch.View;
import net.spy.memcached.protocol.couch.ViewResponseWithDocs;

public interface CouchbaseClientIF {

	// View Access
	public HttpFuture<View> asyncGetView(final String designDocumentName, final String viewName);

	public HttpFuture<List<View>> asyncGetViews(final String designDocumentName);

	public View getView(final String designDocumentName, final String viewName);

	public List<View> getViews(final String designDocumentName);

	// Query
	public ViewFuture asyncQuery(View view, Query query);

	public HttpFuture<ViewResponseNoDocs> asyncQueryAndExcludeDocs(View view, Query query);

	public HttpFuture<ViewResponseReduced> asyncQueryAndReduce(View view, Query query);

	public ViewResponseWithDocs query(View view, Query query);

	public ViewResponseNoDocs queryAndExcludeDocs(View view, Query query);

	public ViewResponseReduced queryAndReduce(View view, Query query);
}
