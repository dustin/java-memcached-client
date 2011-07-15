package net.spy.memcached;

public class TimeoutTest extends ClientBaseCase {

	@Override
	protected void tearDown() throws Exception {
		// override teardown to avoid the flush phase
		client.shutdown();
	}

	@Override
	protected void initClient() throws Exception {
		client=new MemcachedClient(new DefaultConnectionFactory() {
			@Override
			public long getOperationTimeout() {
				return 20;
			}
			@Override
			public FailureMode getFailureMode() {
				return FailureMode.Retry;
			}},
			AddrUtil.getAddresses(TestConfig.IPV4_ADDR + ":64213"));
	}

	private void tryTimeout(String name, Runnable r) {
		try {
			r.run();
			fail("Expected timeout in " + name);
		} catch(OperationTimeoutException e) {
			// pass
		}
	}

	public void testCasTimeout() {
		tryTimeout("cas", new Runnable() {public void run() {
			client.cas("k", 1, "blah");
		}});
	}

	public void testGetsTimeout() {
		tryTimeout("gets", new Runnable() {public void run() {
			client.gets("k");
		}});
	}

	public void testGetTimeout() {
		tryTimeout("get", new Runnable() {public void run() {
			client.get("k");
		}});
	}

	public void testGetBulkTimeout() {
		tryTimeout("getbulk", new Runnable() {public void run() {
			client.getBulk("k", "k2");
		}});
	}

	public void testIncrTimeout() {
		tryTimeout("incr", new Runnable() {public void run() {
			client.incr("k", 1);
		}});
	}

	public void testIncrWithDefTimeout() {
		tryTimeout("incrWithDef", new Runnable() {public void run() {
			client.incr("k", 1, 5);
		}});
	}

}
