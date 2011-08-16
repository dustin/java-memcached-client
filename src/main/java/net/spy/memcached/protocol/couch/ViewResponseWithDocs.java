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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Holds the response of a view query where the map function was
 * called and the documents are included.
 */
public class ViewResponseWithDocs extends ViewResponse {

  private Map<String, Object> map;

  public ViewResponseWithDocs(final Collection<ViewRow> rows,
      final Collection<RowError> errors) {
    super(rows, errors);
    map = null;
  }

  @Override
  public Map<String, Object> getMap() {
    if (map == null) {
      map = new HashMap<String, Object>();
      Iterator<ViewRow> itr = iterator();

      while(itr.hasNext()) {
        ViewRow cur = itr.next();
        map.put(cur.getId(), cur.getDocument());
      }
    }
    return Collections.unmodifiableMap(map);
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    for (ViewRow r : rows) {
      s.append(r.getId());
      s.append(" : ");
      s.append(r.getKey());
      s.append(" : ");
      s.append(r.getValue());
      s.append(" : ");
      s.append(r.getDocument());
      s.append("\n");
    }
    return s.toString();
  }
}

