package net.spy.memcached.protocol.couchdb;

import java.util.Collection;
import java.util.Iterator;

public class ViewResponseReduced implements ViewResponse<RowReduced> {

	Collection<RowReduced> rows;

	public ViewResponseReduced(Collection<RowReduced> r) {
		rows = r;
	}

	public int size() {
		return rows.size();
	}

	@Override
	public Iterator<RowReduced> iterator() {
		return rows.iterator();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (RowReduced r : rows) {
			s.append(r.getKey() + " : " + r.getValue() + "\n");
		}
		return s.toString();
	}
}
