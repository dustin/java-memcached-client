package net.spy.memcached;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

public abstract class ClientBaseCase extends TestCase {

	protected MemcachedClientWithTranscoder client = null;

	protected void initClient() throws Exception {
		initClient(new DefaultConnectionFactory() {
			@Override
			public long getOperationTimeout() {
				return 15000;
			}
		});
	}

	protected void initClient(ConnectionFactory cf) throws Exception {
		client=new MemcachedClientImpl(cf,
			AddrUtil.getAddresses("127.0.0.1:11211"));
	}

	protected Collection<String> stringify(Collection<?> c) {
		Collection<String> rv=new ArrayList<String>();
		for(Object o : c) {
			rv.add(String.valueOf(o));
		}
		return rv;
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