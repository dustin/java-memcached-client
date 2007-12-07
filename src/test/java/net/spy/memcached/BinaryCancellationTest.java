package net.spy.memcached;

/**
 * Test cancellation in the binary protocol.
 */
public class BinaryCancellationTest extends CancellationBaseCase {

	@Override
	protected void initClient() throws Exception {
		initClient(new BinaryConnectionFactory());
	}

}
