package net.spy.memcached;

import java.nio.ByteBuffer;

import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.protocol.ascii.ExtensibleOperationImpl;

/**
 *
 * @author Matt Ingenthron <ingenthr@cep.net>
 */
public class TimeoutNowriteTest extends ClientBaseCase {

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
				return 1000; // 1 sec
			}
			@Override
			public FailureMode getFailureMode() {
				return FailureMode.Retry;
			}},
			AddrUtil.getAddresses(TestConfig.IPV4_ADDR + ":11211"));
	}

	public void testTimeoutDontwrite() {
		Operation op = new ExtensibleOperationImpl(new OperationCallback(){
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
			}

			};
	try {
	    Thread.sleep(1100);
	} catch (InterruptedException ex) {
	    System.err.println("Interrupted when sleeping for timeout nowrite");
	}

	client.addOp("x", op);
	System.err.println("Operation attempted:");
	System.err.println(op);
	System.err.println("Trying to get:");
	try {
		client.get("x");
		String retValString = new String();
		System.err.println(retValString);
	}
	catch (net.spy.memcached.OperationTimeoutException ex) {
		System.err.println("Timed out successfully: " + ex.getMessage());
	}

	System.err.println("Op timed out is " + op.isTimedOut());
	assert(op.isTimedOut() == true);
	}

}
