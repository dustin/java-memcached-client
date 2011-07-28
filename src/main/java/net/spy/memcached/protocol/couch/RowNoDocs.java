package net.spy.memcached.protocol.couch;

public class RowNoDocs extends RowReduced {
	private String id;

	public RowNoDocs(String id, String key, String value) {
		super(key, value);
		// The id can be the string "null" so convert it to null
		if (id != null && id.equals("null")) {
			this.id = null;
		} else {
			this.id = id;
		}
	}

	public String getId() {
		return id;
	}
}
