package net.spy.memcached.protocol.couch;

import java.util.Collection;
import java.util.Iterator;

public class ViewResponseReduced implements ViewResponse<RowReduced> {

	final Collection<RowReduced> rows;
	final Collection<RowError> errors;

	public ViewResponseReduced(final Collection<RowReduced> r,
			final Collection<RowError> e) {
		rows = r;
		errors = e;
	}

	public Collection<RowError> getErrors() {
		return errors;
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
