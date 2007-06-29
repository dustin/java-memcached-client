package net.spy.memcached;

import junit.framework.TestCase;

public abstract class ClientBaseCase extends TestCase {

	protected MemcachedClient client = null;

	private void initClient() throws Exception {
		initClient(new DefaultConnectionFactory());
	}

	protected void initClient(ConnectionFactory cf) throws Exception {
		client=new MemcachedClient(cf,
			AddrUtil.getAddresses("127.0.0.1:11211"));
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		initClient();
	}

	@Override
	protected void tearDown() throws Exception {
		// Shut down, start up, flush, and shut down again.  Error tests have
		// unpredictable timing issues.
		client.shutdown();
		client=null;
		initClient();
		flushPause();
		assertTrue(client.flush().get());
		client.shutdown();
		client=null;
		super.tearDown();
	}

	protected void flushPause() throws InterruptedException {
		// nothing useful
	}

}