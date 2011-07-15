package net.spy.memcached;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.protocol.ascii.ExtensibleOperationImpl;

/**
 * This test assumes a server is running on the host specified
 * in the environment variable SPY_MC_TEST_SERVER or
 * localhost:11211 by default.
 */
public class AsciiClientTest extends ProtocolBaseCase {

	public void testBadOperation() throws Exception {
		client.addOp("x", new ExtensibleOperationImpl(new OperationCallback(){
			public void complete() {
				System.err.println("Complete.");
			}

			public void receivedStatus(OperationStatus s) {
				System.err.println("Received a line.");
			}}) {

			@Override
			public void handleLine(String line) {
				System.out.println("Woo! A line!");
			}

			@Override
			public void initialize() {
				setBuffer(ByteBuffer.wrap("garbage\r\n".getBytes()));
			}});
	}

	@Override
	protected String getExpectedVersionSource() {
		return String.valueOf(
				new InetSocketAddress(TestConfig.IPV4_ADDR, 11211));
	}

}
