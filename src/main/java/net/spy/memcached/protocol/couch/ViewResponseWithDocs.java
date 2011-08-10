/**
 * Copyright (C) 2009-2011 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */

package net.spy.memcached.protocol.couch;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Holds the response of a view query where the map function was
 * called and the documents are included.
 */
public class ViewResponseWithDocs implements ViewResponse,
    Map<String, Object> {

  private final Map<String, Object> map;
  private final Collection<ViewRow> rows;
  private final Collection<RowError> errors;

  public ViewResponseWithDocs(final Collection<ViewRow> r,
      final Collection<RowError> e) {
    map = new HashMap<String, Object>();
    rows = r;
    errors = e;
    for (ViewRow row : rows) {
      map.put(row.getId(), row.getDocument());
    }
  }

  public void addError(RowError r) {
    errors.add(r);
  }

  public Collection<RowError> getErrors() {
    return errors;
  }

  @Override
  public Iterator<ViewRow> iterator() {
    return rows.iterator();
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    for (ViewRow r : rows) {
      s.append(r.getId() + " : " + r.getKey() + " : " + r.getValue() + " : "
          + r.getDocument() + "\n");
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
    for (ViewRow r : rows) {
      set.add(new ViewResponseEntry<String, Object>(r.getId(),
          map.get(r.getId())));
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
    for (ViewRow r : rows) {
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
    for (ViewRow r : rows) {
      values.add(r.getDocument());
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
    public V setValue(V newValue) {
      V old = this.value;
      this.value = newValue;
      return old;
    }
  }
}
