package net.spy.memcached.protocol.couch;

import java.util.Collection;
import java.util.Iterator;

public class ViewResponseNoDocs implements ViewResponse<RowNoDocs> {

	final Collection<RowNoDocs> rows;
	final Collection<RowError> errors;

	public ViewResponseNoDocs(final Collection<RowNoDocs> r,
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
