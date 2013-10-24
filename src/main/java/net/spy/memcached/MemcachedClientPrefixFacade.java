package net.spy.memcached;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.NodeLocator;
import net.spy.memcached.internal.BulkFuture;
import net.spy.memcached.transcoders.Transcoder;

public class MemcachedClientPrefixFacade implements MemcachedClientIF {
	private MemcachedClientIF delegate;
	private String prefix;

	public MemcachedClientPrefixFacade(MemcachedClientIF delegate, String prefix) {
		this.delegate = delegate;
		this.prefix = prefix;
	}

	@Override
	public boolean shutdown(long timeout, TimeUnit unit) {
		return delegate.shutdown(timeout, unit);
	}

	@Override
	public Future<Boolean> flush() {
		return delegate.flush();
	}

	@Override
	public Future<Boolean> flush(int delay) {
		return delegate.flush(delay);
	}

	@Override
	public Collection<SocketAddress> getAvailableServers() {
		return delegate.getAvailableServers();
	}

	@Override
	public Collection<SocketAddress> getUnavailableServers() {
		return delegate.getUnavailableServers();
	}

	@Override
	public NodeLocator getNodeLocator() {
		return delegate.getNodeLocator();
	}

	@Override
	public Transcoder<Object> getTranscoder() {
		return delegate.getTranscoder();
	}

	@Override
	public <T> Future<Boolean> touch(String key, int exp) {
		return delegate.touch(prefix + key, exp);
	}

	@Override
	public <T> Future<Boolean> touch(String key, int exp, Transcoder<T> tc) {
		return delegate.touch(prefix + key, exp, tc);
	}

	@Override
	public Future<Boolean> append(long cas, String key, Object val) {
		return delegate.append(cas, prefix + key, val);
	}

	@Override
	public <T> Future<Boolean> append(long cas, String key, T val, Transcoder<T> tc) {
		return delegate.append(cas, prefix + key, val, tc);
	}

	@Override
	public Future<Boolean> append(String key, Object val) {
		return delegate.append(prefix + key, val);
	}

	@Override
	public <T> Future<Boolean> append(String key, T val, Transcoder<T> tc) {
		return delegate.append(prefix + key, val, tc);
	}

	@Override
	public Future<Boolean> prepend(long cas, String key, Object val) {
		return delegate.prepend(cas, prefix + key, val);
	}

	@Override
	public <T> Future<Boolean> prepend(long cas, String key, T val, Transcoder<T> tc) {
		return delegate.prepend(cas, prefix + key, val, tc);
	}

	@Override
	public Future<Boolean> prepend(String key, Object val) {
		return delegate.prepend(prefix + key, val);
	}

	@Override
	public <T> Future<Boolean> prepend(String key, T val, Transcoder<T> tc) {
		return delegate.prepend(prefix + key, val, tc);
	}

	@Override
	public <T> Future<CASResponse> asyncCAS(String key, long casId, T value, Transcoder<T> tc) {
		return delegate.asyncCAS(prefix + key, casId, value, tc);
	}

	@Override
	public Future<CASResponse> asyncCAS(String key, long casId, Object value) {
		return delegate.asyncCAS(prefix + key, casId, value);
	}

	@Override
	public Future<CASResponse> asyncCAS(String key, long casId, int exp, Object value) {
		return delegate.asyncCAS(prefix + key, casId, exp, value);
	}

	@Override
	public <T> CASResponse cas(String key, long casId, int exp, T value, Transcoder<T> tc) {
		return delegate.cas(prefix + key, casId, exp, value, tc);
	}

	@Override
	public CASResponse cas(String key, long casId, Object value) {
		return delegate.cas(prefix + key, casId, value);
	}

	@Override
	public CASResponse cas(String key, long casId, int exp, Object value) {
		return delegate.cas(prefix + key, casId, exp, value);
	}

	@Override
	public <T> Future<Boolean> add(String key, int exp, T o, Transcoder<T> tc) {
		return delegate.add(prefix + key, exp, o, tc);
	}

	@Override
	public Future<Boolean> add(String key, int exp, Object o) {
		return delegate.add(prefix + key, exp, o);
	}

	@Override
	public <T> Future<Boolean> set(String key, int exp, T o, Transcoder<T> tc) {
		return delegate.set(prefix + key, exp, o, tc);
	}

	@Override
	public Future<Boolean> set(String key, int exp, Object o) {
		return delegate.set(prefix + key, exp, o);
	}

	@Override
	public <T> Future<Boolean> replace(String key, int exp, T o, Transcoder<T> tc) {
		return delegate.replace(prefix + key, exp, o, tc);
	}

	@Override
	public Future<Boolean> replace(String key, int exp, Object o) {
		return delegate.replace(prefix + key, exp, o);
	}

	@Override
	public <T> Future<T> asyncGet(String key, Transcoder<T> tc) {
		return delegate.asyncGet(prefix + key, tc);
	}

	@Override
	public Future<Object> asyncGet(String key) {
		return delegate.asyncGet(prefix + key);
	}

	@Override
	public <T> Future<CASValue<T>> asyncGets(String key, Transcoder<T> tc) {
		return delegate.asyncGets(prefix + key, tc);
	}

	@Override
	public Future<CASValue<Object>> asyncGets(String key) {
		return delegate.asyncGets(prefix + key);
	}

	@Override
	public <T> CASValue<T> gets(String key, Transcoder<T> tc) {
		return delegate.gets(prefix + key, tc);
	}

	@Override
	public <T> CASValue<T> getAndTouch(String key, int exp, Transcoder<T> tc) {
		return delegate.getAndTouch(prefix + key, exp, tc);
	}

	@Override
	public CASValue<Object> getAndTouch(String key, int exp) {
		return delegate.getAndTouch(prefix + key, exp);
	}

	@Override
	public CASValue<Object> gets(String key) {
		return delegate.gets(prefix + key);
	}

	@Override
	public <T> T get(String key, Transcoder<T> tc) {
		return delegate.get(prefix + key, tc);
	}

	@Override
	public Object get(String key) {
		return delegate.get(prefix + key);
	}

	@Override
	public <T> BulkFuture<Map<String, T>> asyncGetBulk(Iterator<String> keyIter, Iterator<Transcoder<T>> tcIter) {
		return delegate.asyncGetBulk(prefixIterator(keyIter), tcIter);
	}

	@Override
	public <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys, Iterator<Transcoder<T>> tcIter) {
		return delegate.asyncGetBulk(prefixCollection(keys), tcIter);
	}

	@Override
	public <T> BulkFuture<Map<String, T>> asyncGetBulk(Iterator<String> keyIter, Transcoder<T> tc) {
		return delegate.asyncGetBulk(prefixIterator(keyIter), tc);
	}

	@Override
	public <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys, Transcoder<T> tc) {
		return delegate.asyncGetBulk(prefixCollection(keys), tc);
	}

	@Override
	public BulkFuture<Map<String, Object>> asyncGetBulk(Iterator<String> keyIter) {
		return delegate.asyncGetBulk(prefixIterator(keyIter));
	}

	@Override
	public BulkFuture<Map<String, Object>> asyncGetBulk(Collection<String> keys) {
		return delegate.asyncGetBulk(prefixCollection(keys));
	}

	@Override
	public <T> BulkFuture<Map<String, T>> asyncGetBulk(Transcoder<T> tc, String... keys) {
		return delegate.asyncGetBulk(tc, prefixArray(keys));
	}

	@Override
	public BulkFuture<Map<String, Object>> asyncGetBulk(String... keys) {
		return delegate.asyncGetBulk(prefixArray(keys));
	}

	@Override
	public Future<CASValue<Object>> asyncGetAndTouch(String key, int exp) {
		return delegate.asyncGetAndTouch(prefix + key, exp);
	}

	@Override
	public <T> Future<CASValue<T>> asyncGetAndTouch(String key, int exp, Transcoder<T> tc) {
		return delegate.asyncGetAndTouch(prefix + key, exp, tc);
	}

	@Override
	public <T> Map<String, T> getBulk(Iterator<String> keyIter, Transcoder<T> tc) {
		return delegate.getBulk(prefixIterator(keyIter), tc);
	}

	@Override
	public Map<String, Object> getBulk(Iterator<String> keyIter) {
		return delegate.getBulk(prefixIterator(keyIter));
	}

	@Override
	public <T> Map<String, T> getBulk(Collection<String> keys, Transcoder<T> tc) {
		return delegate.getBulk(prefixCollection(keys), tc);
	}

	@Override
	public Map<String, Object> getBulk(Collection<String> keys) {
		return delegate.getBulk(prefixCollection(keys));
	}

	@Override
	public <T> Map<String, T> getBulk(Transcoder<T> tc, String... keys) {
		return delegate.getBulk(tc, prefixArray(keys));
	}

	@Override
	public Map<String, Object> getBulk(String... keys) {
		return delegate.getBulk(prefixArray(keys));
	}

	@Override
	public Map<SocketAddress, String> getVersions() {
		return delegate.getVersions();
	}

	@Override
	public Map<SocketAddress, Map<String, String>> getStats() {
		return delegate.getStats();
	}

	@Override
	public Map<SocketAddress, Map<String, String>> getStats(String arg) {
		return delegate.getStats(arg);
	}

	@Override
	public long incr(String key, long by) {
		return delegate.incr(prefix + key, by);
	}

	@Override
	public long incr(String key, int by) {
		return delegate.incr(prefix + key, by);
	}

	@Override
	public long decr(String key, long by) {
		return delegate.decr(prefix + key, by);
	}

	@Override
	public long decr(String key, int by) {
		return delegate.decr(prefix + key, by);
	}

	@Override
	public long incr(String key, long by, long def, int exp) {
		return delegate.incr(prefix + key, by, def, exp);
	}

	@Override
	public long incr(String key, int by, long def, int exp) {
		return delegate.incr(prefix + key, by, def, exp);
	}

	@Override
	public long decr(String key, long by, long def, int exp) {
		return delegate.decr(prefix + key, by, def, exp);
	}

	@Override
	public long decr(String key, int by, long def, int exp) {
		return delegate.decr(prefix + key, by, def, exp);
	}

	@Override
	public Future<Long> asyncIncr(String key, long by) {
		return delegate.asyncIncr(prefix + key, by);
	}

	@Override
	public Future<Long> asyncIncr(String key, int by) {
		return delegate.asyncIncr(prefix + key, by);
	}

	@Override
	public Future<Long> asyncDecr(String key, long by) {
		return delegate.asyncDecr(prefix + key, by);
	}

	@Override
	public Future<Long> asyncDecr(String key, int by) {
		return delegate.asyncDecr(prefix + key, by);
	}

	@Override
	public long incr(String key, long by, long def) {
		return delegate.incr(prefix + key, by, def);
	}

	@Override
	public long incr(String key, int by, long def) {
		return delegate.incr(prefix + key, by, def);
	}

	@Override
	public long decr(String key, long by, long def) {
		return delegate.decr(prefix + key, by, def);
	}

	@Override
	public long decr(String key, int by, long def) {
		return delegate.decr(prefix + key, by, def);
	}

	@Override
	public Future<Boolean> delete(String key) {
		return delegate.delete(prefix + key);
	}

	@Override
	public Future<Boolean> delete(String key, long cas) {
		return delegate.delete(prefix + key, cas);
	}

	@Override
	public Set<String> listSaslMechanisms() {
		return delegate.listSaslMechanisms();
	}

	@Override
	public void shutdown() {
		delegate.shutdown();
	}

	@Override
	public boolean waitForQueues(long timeout, TimeUnit unit) {
		return delegate.waitForQueues(timeout, unit);
	}

	@Override
	public boolean addObserver(ConnectionObserver obs) {
		return delegate.addObserver(obs);
	}

	@Override
	public boolean removeObserver(ConnectionObserver obs) {
		return delegate.removeObserver(obs);
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

	private Collection<String> prefixCollection(Collection<String> keys) {
		Collection<String> result = new ArrayList<String>();
		for (String key : keys) {
			result.add(prefix + key);
		}
		return result;
	}

	private Iterator<String> prefixIterator(Iterator<String> iterator) {
		Collection<String> result = new ArrayList<String>();
		while (iterator.hasNext()) {
			result.add(prefix + iterator.next());
		}
		return result.iterator();
	}

	private String[] prefixArray(String[] keys) {
		Collection<String> result = new ArrayList<String>();
		for (String key : keys) {
			result.add(prefix + key);
		}
		return result.toArray(new String[] {});
	}
}