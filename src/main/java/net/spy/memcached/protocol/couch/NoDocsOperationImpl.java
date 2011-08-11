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

import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.http.HttpRequest;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Implementation of a view that calls the map
 * function and excludes the documents in the result.
 */
public class NoDocsOperationImpl extends ViewOperationImpl{

  public NoDocsOperationImpl(HttpRequest r, ViewCallback cb) {
    super(r, cb);
  }

  protected ViewResponseNoDocs parseResult(String json)
    throws ParseException {
    final Collection<ViewRow> rows = new LinkedList<ViewRow>();
    final Collection<RowError> errors = new LinkedList<RowError>();
    if (json != null) {
      try {
        JSONObject base = new JSONObject(json);
        if (base.has("rows")) {
          JSONArray ids = base.getJSONArray("rows");
          for (int i = 0; i < ids.length(); i++) {
            JSONObject elem = ids.getJSONObject(i);
            String id = elem.getString("id");
            String key = elem.getString("key");
            String value = elem.getString("value");
            rows.add(new ViewRowNoDocs(id, key, value));
          }
        }
        if (base.has("errors")) {
          JSONArray ids = base.getJSONArray("errors");
          for (int i = 0; i < ids.length(); i++) {
            JSONObject elem = ids.getJSONObject(i);
            String from = elem.getString("from");
            String reason = elem.getString("reason");
            errors.add(new RowError(from, reason));
          }
        }
      } catch (JSONException e) {
        throw new ParseException("Cannot read json: " + json, 0);
      }
    }
    return new ViewResponseNoDocs(rows, errors);
  }
}
