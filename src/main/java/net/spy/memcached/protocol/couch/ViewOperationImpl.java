package net.spy.memcached.protocol.couch;

import java.net.HttpURLConnection;
import java.text.ParseException;
import java.util.Iterator;

import net.spy.memcached.ops.OperationErrorType;
import net.spy.memcached.ops.OperationException;
import net.spy.memcached.ops.OperationStatus;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class ViewOperationImpl extends HttpOperationImpl implements
		ViewOperation {

	private final String bucketName;
	private final String designDocName;
	private final String viewName;

	public ViewOperationImpl(HttpRequest r, String bucketName,
			String designDocName, String viewName, ViewCallback viewCallback) {
		super(r, viewCallback);
		this.bucketName = bucketName;
		this.designDocName = designDocName;
		this.viewName = viewName;
	}

	@Override
	public void handleResponse(HttpResponse response) {
		String json = getEntityString(response);
		try {
			View view = parseDesignDocumentForView(bucketName, designDocName,
					viewName, json);

			int errorcode = response.getStatusLine().getStatusCode();
			if (errorcode == HttpURLConnection.HTTP_OK) {
				((ViewCallback) callback).gotData(view);
				callback.receivedStatus(new OperationStatus(true, "OK"));
			} else {
				callback.receivedStatus(new OperationStatus(false, Integer
						.toString(errorcode)));
			}
		} catch (ParseException e) {
			exception = new OperationException(OperationErrorType.GENERAL,
					"Error parsing JSON");
		}
		callback.complete();
	}

	private View parseDesignDocumentForView(String dn, String ddn,
			String viewName, String json) throws ParseException {
		View view = null;
		if (json != null) {
			try {
				JSONObject base = new JSONObject(json);
				if (base.has("error")) {
					return null;
				}
				if (base.has("views")) {
					JSONObject views = base.getJSONObject("views");
					Iterator<?> itr = views.keys();
					while (itr.hasNext()) {
						String curView = (String) itr.next();
						if (curView.equals(viewName)) {
							boolean map = views.getJSONObject(curView).has(
									"map");
							boolean reduce = views.getJSONObject(curView).has(
									"reduce");
							view = new View(dn, ddn, viewName, map, reduce);
							break;
						}
					}
				}
			} catch (JSONException e) {
				throw new ParseException("Cannot read json: " + json, 0);
			}
		}
		return view;
	}
}
