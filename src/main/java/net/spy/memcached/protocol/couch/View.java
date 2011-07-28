package net.spy.memcached.protocol.couch;

public class View {
	private final String viewName;
	private final String designDocumentName;
	private final String databaseName;
	private final boolean map;
	private final boolean reduce;

	protected View(String dn, String ddn, String vn, boolean m, boolean r) {
		databaseName = dn;
		designDocumentName = ddn;
		viewName = vn;
		map = m;
		reduce = r;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public String getDesignDocumentName() {
		return designDocumentName;
	}

	public String getViewName() {
		return viewName;
	}

	public boolean hasMap() {
		return map;
	}

	public boolean hasReduce() {
		return reduce;
	}

	public String getURI() {
		return "/" + databaseName + "/_design/" + designDocumentName
				+ "/_view/" + viewName;
	}
}
