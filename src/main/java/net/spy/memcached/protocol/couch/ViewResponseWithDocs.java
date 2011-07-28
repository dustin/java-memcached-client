package net.spy.memcached.protocol.couch;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ViewResponseWithDocs implements ViewResponse<RowWithDocs>,
		Map<String, Object> {

	final Map<String, Object> map;
	final Collection<RowWithDocs> rows;
	final Collection<RowError> errors;

	public ViewResponseWithDocs(final Collection<RowWithDocs> r,
			final Collection<RowError> e) {
		map = new HashMap<String, Object>();
		rows = r;
		errors = e;
		for (RowWithDocs row : rows) {
			map.put(row.getId(), row.getDoc());
		}
	}

	public void addError(RowError r) {
		errors.add(r);
	}

	public Collection<RowError> getErrors() {
		return errors;
	}

	@Override
	public Iterator<RowWithDocs> iterator() {
		return rows.iterator();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (RowWithDocs r : rows) {
			s.append(r.getId() + " : " + r.getKey() + " : " + r.getValue()
					+ " : " + r.getDoc() + "\n");
		}
		return s.toString();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("clear() is not supported");
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		Set<Entry<String, Object>> set = new HashSet<Entry<String, Object>>();
		for (RowWithDocs r : rows) {
			set.add(new ViewResponseEntry<String, Object>(r.getId(), map.get(r
					.getId())));
		}
		return null;
	}

	@Override
	public Object get(Object key) {
		return map.get(key);
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		Set<String> set = new TreeSet<String>();
		for (RowWithDocs r : rows) {
			set.add(r.getId());
		}
		return null;
	}

	@Override
	public Object put(String key, Object value) {
		throw new UnsupportedOperationException("put() is not supported");
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		throw new UnsupportedOperationException("putAll() is not supported");

	}

	@Override
	public Object remove(Object key) {
		throw new UnsupportedOperationException("remove() is not supported");
	}

	@Override
	public int size() {
		assert rows.size() == map.size();
		return rows.size();
	}

	@Override
	public Collection<Object> values() {
		Collection<Object> values = new LinkedList<Object>();
		for (RowWithDocs r : rows) {
			values.add(r.getDoc());
		}
		return values;
	}

	final class ViewResponseEntry<K, V> implements Map.Entry<K, V> {
		private final K key;
		private V value;

		public ViewResponseEntry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			V old = this.value;
			this.value = value;
			return old;
		}
	}

}
