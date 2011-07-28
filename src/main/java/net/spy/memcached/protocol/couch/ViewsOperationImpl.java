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

public class ViewsOperationImpl extends HttpOperationImpl implements
		ViewsOperation {

	private final String bucketName;
	private final String designDocName;

	public ViewsOperationImpl(HttpRequest r, String bucketName,
			String designDocName, ViewsCallback viewsCallback) {
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
				((ViewsCallback) callback).gotData(views);
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
					boolean reduce = views.getJSONObject(viewname)
							.has("reduce");
					viewList.add(new View(dn, ddn, viewname, map, reduce));
				}
			}
		} catch (JSONException e) {
			throw new ParseException("Cannot read json: " + json, 0);
		}
		return viewList;
	}
}
