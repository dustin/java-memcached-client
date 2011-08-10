/**
 * Copyright (C) 2009-2011 Couchbase, Inc.
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

import java.util.List;

import net.spy.memcached.internal.HttpFuture;
import net.spy.memcached.protocol.couch.Query;
import net.spy.memcached.protocol.couch.View;
import net.spy.memcached.protocol.couch.ViewResponseNoDocs;
import net.spy.memcached.protocol.couch.ViewResponseReduced;
import net.spy.memcached.protocol.couch.ViewResponseWithDocs;

/**
 * This interface is provided as a helper for testing clients of the
 * CouchbaseClient.
 */
public interface CouchbaseClientIF extends MembaseClientIF {

  // View Access
  HttpFuture<View> asyncGetView(final String designDocumentName,
      final String viewName);

  HttpFuture<List<View>> asyncGetViews(final String designDocumentName);

  View getView(final String designDocumentName, final String viewName);

  List<View> getViews(final String designDocumentName);

  // Query
  HttpFuture<ViewResponseWithDocs> asyncQuery(View view, Query query);

  HttpFuture<ViewResponseNoDocs> asyncQueryAndExcludeDocs(View view,
      Query query);

  HttpFuture<ViewResponseReduced> asyncQueryAndReduce(View view,
      Query query);

  ViewResponseWithDocs query(View view, Query query);

  ViewResponseNoDocs queryAndExcludeDocs(View view, Query query);

  ViewResponseReduced queryAndReduce(View view, Query query);
}
