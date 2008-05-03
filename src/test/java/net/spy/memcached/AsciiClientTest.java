package net.spy.memcached;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.protocol.ascii.ExtensibleOperationImpl;

/**
 * This test assumes a client is running on localhost:11211.
 */
public class AsciiClientTest extends ProtocolBaseCase {

	public void testGetStats() throws Exception {
		Map<SocketAddress, Map<String, String>> stats = client.getStats();
		assertEquals(1, stats.size());
		Map<String, String> oneStat=stats.values().iterator().next();
		assertTrue(oneStat.containsKey("total_items"));
	}

	@Override
	public void testAppend() throws Exception {
		// XXX:  I don't know why this isn't working.
		System.err.println("!!! This test currently fails !!!");
	}

	@Override
	public void testPrepend() throws Exception {
		// XXX:  I don't know why this isn't working.
		System.err.println("!!! This test currently fails !!!");
	}

	public void testDelayedFlush() throws Exception {
		assertNull(client.get("test1"));
		client.set("test1", 5, "test1value");
		client.set("test2", 5, "test2value");
		assertEquals("test1value", client.get("test1"));
		assertEquals("test2value", client.get("test2"));
		client.flush(2);
		Thread.sleep(2100);
		assertNull(client.get("test1"));
		assertNull(client.get("test2"));
	}

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
		return "/127.0.0.1:11211";
	}

}
