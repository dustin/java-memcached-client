package net.spy.memcached;

/**
 * This test assumes a binary server is running on localhost:11211.
 */
public class BinaryClientTest extends ProtocolBaseCase {

	@Override
	protected void initClient() throws Exception {
		initClient(new BinaryConnectionFactory() {
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

	@Override
	protected String getExpectedVersionSource() {
		return "/127.0.0.1:11211";
	}

	@Override
	public void testGetStatsCacheDump() throws Exception {
		// XXX:  Cachedump isn't returning anything from the server in binprot
		assertTrue(true);
	}

	public void testCASAppendFail() throws Exception {
		final String key="append.key";
		assertTrue(client.set(key, 5, "test").get());
		CASValue<Object> casv = client.gets(key);
		assertFalse(client.append(casv.getCas() + 1, key, "es").get());
		assertEquals("test", client.get(key));
	}

	public void testCASAppendSuccess() throws Exception {
		final String key="append.key";
		assertTrue(client.set(key, 5, "test").get());
		CASValue<Object> casv = client.gets(key);
		assertTrue(client.append(casv.getCas(), key, "es").get());
		assertEquals("testes", client.get(key));
	}

	public void testCASPrependFail() throws Exception {
		final String key="append.key";
		assertTrue(client.set(key, 5, "test").get());
		CASValue<Object> casv = client.gets(key);
		assertFalse(client.prepend(casv.getCas() + 1, key, "es").get());
		assertEquals("test", client.get(key));
	}

	public void testCASPrependSuccess() throws Exception {
		final String key="append.key";
		assertTrue(client.set(key, 5, "test").get());
		CASValue<Object> casv = client.gets(key);
		assertTrue(client.prepend(casv.getCas(), key, "es").get());
		assertEquals("estest", client.get(key));
	}

	public void testGATTimeout() throws Exception {
		if (TestConfig.isMembase()) {
			assertNull(client.get("gatkey"));
			assert client.set("gatkey", 1, "gatvalue").get().booleanValue();
			assert client.getAndTouch("gatkey", 2).getValue().equals("gatvalue");
			Thread.sleep(1300);
			assert client.get("gatkey").equals("gatvalue");
			Thread.sleep(2000);
			assertNull(client.getAndTouch("gatkey", 3));
		}
	}

	public void testTouchTimeout() throws Exception {
		if (TestConfig.isMembase()) {
			assertNull(client.get("touchkey"));
			assert client.set("touchkey", 1, "touchvalue").get().booleanValue();
			assert client.touch("touchkey", 2).get().booleanValue();
			Thread.sleep(1300);
			assert client.get("touchkey").equals("touchvalue");
			Thread.sleep(2000);
			assertFalse(client.touch("touchkey", 3).get().booleanValue());
		}
	}

	@Override
	protected void syncGetTimeoutsInitClient() throws Exception {
		initClient(new BinaryConnectionFactory() {
			@Override
			public long getOperationTimeout() {
				return 2;
			}

			@Override
			public int getTimeoutExceptionThreshold() {
				return 1000000;
			}
		});
	}
}
