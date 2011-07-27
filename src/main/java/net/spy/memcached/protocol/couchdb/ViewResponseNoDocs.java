package net.spy.memcached.protocol.couchdb;

import java.util.Collection;
import java.util.Iterator;

public class ViewResponseNoDocs implements ViewResponse<RowNoDocs> {

	Collection<RowNoDocs> rows;

	public ViewResponseNoDocs(final Collection<RowNoDocs> r) {
		rows = r;
	}

	public int size() {
		return rows.size();
	}

	@Override
	public Iterator<RowNoDocs> iterator() {
		return rows.iterator();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (RowNoDocs r : rows) {
			s.append(r.getId() + " : " + r.getKey() + " : " + r.getValue()
					+ "\n");
		}
		return s.toString();
	}
}
