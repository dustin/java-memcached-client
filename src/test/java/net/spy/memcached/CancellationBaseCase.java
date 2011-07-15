package net.spy.memcached;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
			AddrUtil.getAddresses(TestConfig.IPV4_ADDR + ":64213"));
	}

	private void tryCancellation(Future<?> f) throws Exception {
		f.cancel(true);
		assertTrue(f.isCancelled());
		assertTrue(f.isDone());
		try {
			Object o=f.get();
			fail("Expected cancellation, got " + o);
		} catch (ExecutionException e) {
			assertTrue(e.getCause() instanceof RuntimeException);
			assertEquals("Cancelled", e.getCause().getMessage());
		}
	}

	public void testAvailableServers() {
		client.asyncGet("x");
		assertEquals(Collections.emptyList(), client.getAvailableServers());
	}

	public void testUnavailableServers() {
		client.asyncGet("x");
		assertEquals(new ArrayList<String>(
				Collections.singleton("/" + TestConfig.IPV4_ADDR + ":64213")),
			stringify(client.getUnavailableServers()));
	}

	private void tryTimeout(Future<?> f) throws Exception {
		try {
			Object o=f.get(10, TimeUnit.MILLISECONDS);
			fail("Expected timeout, got " + o);
		} catch(TimeoutException e) {
			// expected
		}
	}

	protected void tryTestSequence(Future<?> f) throws Exception {
		tryTimeout(f);
		tryCancellation(f);
	}

	public void testAsyncGetCancellation() throws Exception {
		tryTestSequence(client.asyncGet("k"));
	}

	public void testAsyncGetsCancellation() throws Exception {
		tryTestSequence(client.asyncGets("k"));
	}

	public void testAsyncGetBulkCancellationCollection() throws Exception {
		tryTestSequence(client.asyncGetBulk(Arrays.asList("k", "k2")));
	}

	public void testAsyncGetBulkCancellationVararg() throws Exception {
		tryTestSequence(client.asyncGetBulk("k", "k2"));
	}

	public void testDeleteCancellation() throws Exception {
		tryTestSequence(client.delete("x"));
	}

	public void testflushCancellation() throws Exception {
		tryTestSequence(client.flush());
	}

	public void testDelayedflushCancellation() throws Exception {
		tryTestSequence(client.flush(3));
	}

	public void testReplaceCancellation() throws Exception {
		tryTestSequence(client.replace("x", 3, "y"));
	}

	public void testAddCancellation() throws Exception {
		tryTestSequence(client.add("x", 3, "y"));
	}

	public void testSetCancellation() throws Exception {
		tryTestSequence(client.set("x", 3, "y"));
	}

	public void testCASCancellation() throws Exception {
		tryTestSequence(client.asyncCAS("x", 3, "y"));
	}
}
