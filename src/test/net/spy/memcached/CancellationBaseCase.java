package net.spy.memcached;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Base class for cancellation tests.
 */
public abstract class CancellationBaseCase extends ClientBaseCase {

	@Override
	protected void tearDown() throws Exception {
		// override teardown to avoid the flush phase
		client.shutdown();
	}

	@Override
	protected void initClient(ConnectionFactory cf) throws Exception {
		client=new MemcachedClient(cf,
			AddrUtil.getAddresses("127.0.0.1:11213"));
	}

	protected void tryCancellation(Future<?> f) throws Exception {
		f.cancel(true);
		try {
			Object o=f.get();
			fail("Expected cancellation, got " + o);
		} catch (ExecutionException e) {
			assertTrue(e.getCause() instanceof RuntimeException);
			assertEquals("Cancelled", e.getCause().getMessage());
		}
	}

	public void testAsyncGetCancellation() throws Exception {
		tryCancellation(client.asyncGet("k"));
	}

	public void testAsyncGetBulkCancellationCollection() throws Exception {
		tryCancellation(client.asyncGetBulk(Arrays.asList("k", "k2")));
	}

	public void testAsyncGetBulkCancellationVararg() throws Exception {
		tryCancellation(client.asyncGetBulk("k", "k2"));
	}

	public void testDeleteCancellation() throws Exception {
		tryCancellation(client.delete("x"));
	}

	public void testDelayedDeleteCancellation() throws Exception {
		tryCancellation(client.delete("x", 5));
	}

	public void testflushCancellation() throws Exception {
		tryCancellation(client.flush());
	}

	public void testDelayedflushCancellation() throws Exception {
		tryCancellation(client.flush(3));
	}

	public void testReplaceCancellation() throws Exception {
		tryCancellation(client.replace("x", 3, "y"));
	}

	public void testAddCancellation() throws Exception {
		tryCancellation(client.add("x", 3, "y"));
	}

	public void testSetCancellation() throws Exception {
		tryCancellation(client.set("x", 3, "y"));
	}
}
