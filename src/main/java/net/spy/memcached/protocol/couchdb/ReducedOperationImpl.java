package net.spy.memcached.protocol.couchdb;

import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;

import net.spy.memcached.ops.OperationErrorType;
import net.spy.memcached.ops.OperationException;
import net.spy.memcached.ops.OperationStatus;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class ReducedOperationImpl extends HttpOperationImpl implements
		ReducedOperation {

	public ReducedOperationImpl(HttpRequest r, ReducedCallback cb) {
		super(r, cb);
	}

	@Override
	public void handleResponse(HttpResponse response) {
		String json = getEntityString(response);
		int errorcode = response.getStatusLine().getStatusCode();
		try {
			OperationStatus status = parseViewForStatus(json, errorcode);
			ViewResponseReduced vr = parseReducedViewResult(json);

			((ReducedCallback) callback).gotData(vr);
			callback.receivedStatus(status);
		} catch (ParseException e) {
			exception = new OperationException(OperationErrorType.GENERAL,
					"Error parsing JSON");
		}
		callback.complete();
	}

	private ViewResponseReduced parseReducedViewResult(String json)
			throws ParseException {
		final Collection<RowReduced> rows = new LinkedList<RowReduced>();
		if (json != null) {
			try {
				JSONObject base = new JSONObject(json);
				if (base.has("rows")) {
					JSONArray ids = base.getJSONArray("rows");
					for (int i = 0; i < ids.length(); i++) {
						String key = ids.getJSONObject(i).getString("key");
						String value = ids.getJSONObject(i).getString("value");
						rows.add(new RowReduced(key, value));
					}
				}
			} catch (JSONException e) {
				throw new ParseException("Cannot read json: " + json, 0);
			}
		}
		return new ViewResponseReduced(rows);
	}
}
