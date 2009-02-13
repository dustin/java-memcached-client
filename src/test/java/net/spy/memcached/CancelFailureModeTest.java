package net.spy.memcached;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class CancelFailureModeTest extends ClientBaseCase {
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
				return FailureMode.Cancel;
			}
		});
	}

	@Override
	protected void flushPause() throws InterruptedException {
		Thread.sleep(100);
	}

	public void testQueueingToDownServer() throws Exception {
		Future<Boolean> f=client.add("someKey", 0, "some object");
		try {
			boolean b = f.get();
			fail("Should've thrown an exception, returned " + b);
		} catch (ExecutionException e) {
			// probably OK
		}
		assertTrue(f.isCancelled());
	}
}
