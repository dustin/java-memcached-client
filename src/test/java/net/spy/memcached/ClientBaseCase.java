package net.spy.memcached;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import junit.framework.TestCase;

public abstract class ClientBaseCase extends TestCase {

	protected MemcachedClient client = null;
	protected Boolean membase;
	protected Boolean moxi;

	protected void initClient() throws Exception {
		initClient(new DefaultConnectionFactory() {
			@Override
			public long getOperationTimeout() {
				return 15000;
			}
			@Override
			public FailureMode getFailureMode() {
				return FailureMode.Retry;
			}
		});
	}

	protected void initClient(ConnectionFactory cf) throws Exception {
		client=new MemcachedClient(cf,
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

	protected boolean isMoxi() {
	    if (moxi != null) {
		    return moxi.booleanValue();
	    }
		// some tests are invalid if using moxi

		Map<SocketAddress, Map<String, String>> stats = client.getStats("proxy");
		for (Map<String, String> node : stats.values()) {
			if (node.get("basic:version") != null) {
				moxi = true;
				   System.err.println("Using proxy");
				break;
			} else {
				moxi = false;
				   System.err.println("Not using proxy");
			}

	    }
	    return moxi.booleanValue();
	}

}