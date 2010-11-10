package net.spy.memcached.plugin;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import junit.framework.TestCase;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionObserver;

public class FrontCacheMemcachedClientTest extends TestCase {
	
	protected FrontCacheMemcachedClient client = null;
	protected String key = "spy:front";
	protected String value = "result value";
	
	protected void initClient() throws Exception {
		
		final CountDownLatch latch = new CountDownLatch(1);
		ConnectionObserver observer = new ConnectionObserver() {
			
			@Override
			public void connectionLost(SocketAddress sa) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void connectionEstablished(SocketAddress sa, int reconnectCount) {
				latch.countDown();
				
			}
		};
		
		ConnectionFactoryBuilder cfb = new ConnectionFactoryBuilder()
				.setMaxFrontCacheElements(100).setFrontCacheExpireTime(2).
				setInitialObservers(Collections.singleton(observer));

		client = new FrontCacheMemcachedClient(cfb.build(), AddrUtil.getAddresses("127.0.0.1:11211"));
		
		latch.await();
	}
	
	protected void setDummy() throws Exception {
		Future<Boolean> f = client.set(key, 0, value);
		assertTrue("Stored", f.get());
	}
	
	protected void deleteDummy() throws Exception {
		Future<Boolean> f = client.delete(key);
		assertTrue("Deleted", f.get());
	}
	
	public void testFrontCache() throws Exception {
		setDummy();
		Future<Object> f = client.asyncGet(key);
		Object o = f.get();
		assertEquals(value, o);
		
		deleteDummy();
		// although the item is deleted, you can still fetch the item from ehcache
		// during front cache expire time(in this case, expire time is 2 seconds)
		for (int i=0; i<100; i++) {
			Future<Object> f2 = client.asyncGet(key);
			Object o2 = f2.get();
			assertEquals(value, o2);
		}
		
	}
	
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		initClient();
	}
	
	@Override
	protected void tearDown() throws Exception {
		client.shutdown();
		client=null;
		super.tearDown();
	}
	
}