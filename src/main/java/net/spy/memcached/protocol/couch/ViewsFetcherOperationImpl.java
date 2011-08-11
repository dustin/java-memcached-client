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

import java.net.HttpURLConnection;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.spy.memcached.ops.OperationErrorType;
import net.spy.memcached.ops.OperationException;
import net.spy.memcached.ops.OperationStatus;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * A ViewsOperationImpl.
 */
public class ViewsFetcherOperationImpl extends HttpOperationImpl
    implements ViewsFetcherOperation {

  private final String bucketName;
  private final String designDocName;


  public ViewsFetcherOperationImpl(HttpRequest r, String bucketName,
      String designDocName, ViewsFetcherCallback viewsCallback) {
    super(r, viewsCallback);
    this.bucketName = bucketName;
    this.designDocName = designDocName;
  }

  @Override
  public void handleResponse(HttpResponse response) {
    String json = getEntityString(response);
    try {
      int errorcode = response.getStatusLine().getStatusCode();
      if (errorcode == HttpURLConnection.HTTP_OK) {
        List<View> views = parseDesignDocumentForViews(bucketName,
          designDocName, json);
        ((ViewsFetcherCallback) callback).gotData(views);
        callback.receivedStatus(new OperationStatus(true, "OK"));
      } else {
        callback.receivedStatus(new OperationStatus(false,
            Integer.toString(errorcode)));
      }
    } catch (ParseException e) {
      exception = new OperationException(OperationErrorType.GENERAL,
        "Error parsing JSON");
    }
    callback.complete();
  }

  private List<View> parseDesignDocumentForViews(String dn, String ddn,
      String json) throws ParseException {
    List<View> viewList = new LinkedList<View>();
    try {
      JSONObject base = new JSONObject(json);
      if (base.has("error")) {
        return null;
      }
      if (base.has("views")) {
        JSONObject views = base.getJSONObject("views");
        Iterator<?> itr = views.keys();
        while (itr.hasNext()) {
          String viewname = (String) itr.next();
          boolean map = views.getJSONObject(viewname).has("map");
          boolean reduce = views.getJSONObject(viewname).has("reduce");
          viewList.add(new View(dn, ddn, viewname, map, reduce));
        }
      }
    } catch (JSONException e) {
      throw new ParseException("Cannot read json: " + json, 0);
    }
    return viewList;
  }
}
