package net.spy.memcached.plugin;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.transcoders.Transcoder;

public class FrontCacheMemcachedClient extends MemcachedClient {

	private final String frontCacheName = "front";
	
	public FrontCacheMemcachedClient(ConnectionFactory cf, List<InetSocketAddress> addrs)
		throws IOException {
		super(cf, addrs);
		
		if (cf.getMaxFrantCacheElements() > 0) {
			isFrontCache = true;
			if (CacheManager.getInstance().getCache(frontCacheName) == null) {
				CacheManager.getInstance().addCache(new Cache(frontCacheName, cf.getMaxFrantCacheElements(), 
						MemoryStoreEvictionPolicy.LRU, false, "", false, cf.getFrontCacheExpireTime(), 
						cf.getFrontCacheExpireTime(), false, 60, null));
			}
		} else {
			isFrontCache = false;
		}
		
	}
	
	@Override
	public <T> Future<T> asyncGet(final String key, final Transcoder<T> tc) {

		Element frontElement = null;
		
		if (isFrontCache) {
			frontElement = CacheManager.getInstance().getCache(frontCacheName).get(key);
		}
		
		if (frontElement == null) {
			return super.asyncGet(key, tc);
		} else {
			return new FrontCacheGetFuture<T>(frontElement);
		}
	}
	
	public <T> boolean putFrontCache(String key, Future<T> val, long operationTimeout) {
		if (isFrontCache) { 
			try {
				if (val != null) {
					CacheManager
							.getInstance()
							.getCache(frontCacheName)
							.put(new Element(key, val.get(operationTimeout,
									TimeUnit.MILLISECONDS)));
					return true;
				} else {
					return false;
				}
			} catch (Exception e) {
				getLogger().info("failed to store front cache, errorMessage=%s", e.getMessage());
				return false;
			}
		}
		return false;
	}
	
}
