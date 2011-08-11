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

import java.io.IOException;
import java.text.ParseException;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationErrorType;
import net.spy.memcached.ops.OperationException;
import net.spy.memcached.ops.OperationStatus;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * An HttpOperationImpl.
 */
public abstract class HttpOperationImpl implements HttpOperation {

  private final HttpRequest request;
  protected final OperationCallback callback;
  protected OperationException exception;
  private boolean cancelled;
  private boolean errored;
  private boolean timedOut;

  public HttpOperationImpl(HttpRequest r, OperationCallback cb) {
    request = r;
    callback = cb;
    exception = null;
    cancelled = false;
    errored = false;
    timedOut = false;
  }

  public HttpRequest getRequest() {
    return request;
  }

  public OperationCallback getCallback() {
    return callback;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public boolean hasErrored() {
    return errored;
  }

  public boolean isTimedOut() {
    return timedOut;
  }

  public void cancel() {
    cancelled = true;
  }

  public void timeOut() {
    timedOut = true;
  }

  public OperationException getException() {
    return exception;
  }

  public abstract void handleResponse(HttpResponse response);

  protected String getEntityString(HttpResponse response) {
    if (!isTimedOut() && !hasErrored() && !isCancelled()) {
      try {
        return EntityUtils.toString(response.getEntity());
      } catch (org.apache.http.ParseException e) {
        exception = new OperationException(OperationErrorType.GENERAL,
          "Bad http headers");
        errored = true;
      } catch (IOException e) {
        exception = new OperationException(OperationErrorType.GENERAL,
          "Error reading response");
        errored = true;
      } catch (IllegalArgumentException e) {
        exception = new OperationException(OperationErrorType.GENERAL,
          "No entity");
        errored = true;
      }
    }
    return null;
  }

  protected OperationStatus parseViewForStatus(String json, int errorcode)
    throws ParseException {
    if (json != null) {
      try {
        JSONObject base = new JSONObject(json);
        if (base.has("error")) {
          String error = "Error Code: " + errorcode + " Error: "
            + base.getString("error");
          if (base.has("reason")) {
            error += " Reason: " + base.getString("reason");
          }
          return new OperationStatus(false, error);
        } else {
          return new OperationStatus(true, "Error Code: " + errorcode);
        }
      } catch (JSONException e) {
        throw new ParseException("Cannot read json: " + json, 0);
      }
    }
    return new OperationStatus(false, "Error Code: " + errorcode
        + "No entity");
  }
}
