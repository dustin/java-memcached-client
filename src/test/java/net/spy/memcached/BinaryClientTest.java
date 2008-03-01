package net.spy.memcached;


/**
 * This test assumes a binary server is running on localhost:11212.
 */
public class BinaryClientTest extends ProtocolBaseCase {

	@Override
	protected void initClient() throws Exception {
		initClient(new BinaryConnectionFactory());
	}

	@Override
	protected String getExpectedVersionSource() {
		return "/127.0.0.1:11211";
	}

	public void testStats() {
		try {
			client.getStats();
		} catch(UnsupportedOperationException e) {
			// We don't have stats for the binary protocol yet.
		}
	}
}
