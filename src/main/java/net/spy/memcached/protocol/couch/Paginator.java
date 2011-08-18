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

package net.spy.memcached.protocol.couch;

import java.util.Iterator;

import net.spy.memcached.CouchbaseClient;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.internal.HttpFuture;

/**
 * A Paginator.
 */
public class Paginator extends SpyObject
  implements Iterator<ViewRow> {

  private static final int MIN_RESULTS = 15;

  private final CouchbaseClient client;
  private final Query query;
  private final View view;
  private final int docsPerPage;

  private ViewResponse page;
  private Iterator<ViewRow> pageItr;
  private ViewRow lastRow;
  private int rowsIterated;

  public Paginator(CouchbaseClient client, View view, Query query,
      int numDocs) {
    this.client = client;
    this.view = view;
    this.query = query.copy();
    this.docsPerPage = (MIN_RESULTS > numDocs) ? MIN_RESULTS : numDocs;
    getNextPage(this.query.setLimit(docsPerPage + 1));
  }

  @Override
  public boolean hasNext() {
    if (!pageItr.hasNext() && page.size() < docsPerPage) {
      return false;
    } else if (!(rowsIterated < docsPerPage)) {
      lastRow = pageItr.next();
      query.setStartkeyDocID(lastRow.getId());
      query.setRangeStart(lastRow.getKey());
      getNextPage(query);
    }
    return true;
  }

  @Override
  public ViewRow next() {
    if (rowsIterated <= docsPerPage) {
      rowsIterated++;
      lastRow = pageItr.next();
      return lastRow;
    }
    return null;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Remove is unsupported");
  }

  private HttpFuture<ViewResponse> getNextPage(Query q) {
    if (query.willReduce()) {
      throw new RuntimeException("Pagination is not supported for reduced"
          + " views");
    }
    page = client.query(view, q);
    pageItr = page.iterator();
    rowsIterated = 0;
    return null;
  }
}
