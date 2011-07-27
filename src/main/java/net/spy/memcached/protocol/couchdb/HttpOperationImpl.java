package net.spy.memcached.protocol.couchdb;

import java.io.IOException;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationErrorType;
import net.spy.memcached.ops.OperationException;
import net.spy.memcached.ops.OperationStatus;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

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
			} catch (ParseException e) {
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
					return new OperationStatus(true, "Error Code: "
							+ errorcode);
				}
			} catch (JSONException e) {
				throw new ParseException("Cannot read json: " + json);
			}
		}
		return new OperationStatus(false, "Error Code: " +  errorcode
				+ "No entity");
	}
}
