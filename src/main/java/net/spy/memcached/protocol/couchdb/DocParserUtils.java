package net.spy.memcached.protocol.couchdb;

import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class DocParserUtils {
	public static List<View> parseDesignDocumentForViews(String dn, String ddn, String json)
			throws ParseException {
		List<View> viewList = new LinkedList<View>();
		try {
			JSONObject base = new JSONObject(json);
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
