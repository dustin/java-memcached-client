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

package net.spy.memcached.couch;

import java.net.HttpURLConnection;

import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.protocol.couch.HttpOperationImpl;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

/**
 * A TestOperationImpl.
 */
public class TestOperationImpl extends HttpOperationImpl implements
    TestOperation {

  public TestOperationImpl(HttpRequest r, TestCallback testCallback) {
    super(r, testCallback);
  }

  @Override
  public void handleResponse(HttpResponse response) {
    String json = getEntityString(response);
    int errorcode = response.getStatusLine().getStatusCode();
    if (errorcode == HttpURLConnection.HTTP_OK) {
      ((TestCallback) callback).getData(json);
      callback.receivedStatus(new OperationStatus(true, "OK"));
    } else {
      callback.receivedStatus(new OperationStatus(false,
          Integer.toString(errorcode)));
    }
    callback.complete();
  }

}
