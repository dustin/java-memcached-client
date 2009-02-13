package net.spy.memcached;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class RedistributeFailureModeTest extends ClientBaseCase {

	private String serverList;

	@Override
	protected void setUp() throws Exception {
		serverList="127.0.0.1:11211 127.0.0.1:11311";
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		serverList="127.0.0.1:11211";
		super.tearDown();
	}

	@Override
	protected void initClient(ConnectionFactory cf) throws Exception {
		client=new MemcachedClient(cf, AddrUtil.getAddresses(serverList));
	}

	@Override
	protected void initClient() throws Exception {
		initClient(new DefaultConnectionFactory() {
			@Override
			public FailureMode getFailureMode() {
				return FailureMode.Redistribute;
			}
		});
	}

	@Override
	protected void flushPause() throws InterruptedException {
		Thread.sleep(100);
	}

	// Just to make sure the sequence is being handled correctly
	public void testMixedSetsAndUpdates() throws Exception {
		Collection<Future<Boolean>> futures=new ArrayList<Future<Boolean>>();
		Collection<String> keys=new ArrayList<String>();
		Thread.sleep(100);
		for(int i=0; i<100; i++) {
			String key="k" + i;
			futures.add(client.set(key, 10, key));
			futures.add(client.add(key, 10, "a" + i));
			keys.add(key);
		}
		Map<String, Object> m=client.getBulk(keys);
		assertEquals(100, m.size());
		for(Map.Entry<String, Object> me : m.entrySet()) {
			assertEquals(me.getKey(), me.getValue());
		}
		for(Iterator<Future<Boolean>> i=futures.iterator();i.hasNext();) {
			assertTrue(i.next().get(10, TimeUnit.MILLISECONDS));
			assertFalse(i.next().get(10, TimeUnit.MILLISECONDS));
		}
		System.err.println(getName() + " complete.");
	}
}
