package net.spy.memcached.cas;

import java.util.concurrent.Callable;

import net.spy.memcached.ClientBaseCase;
import net.spy.memcached.cas.CASMutation;
import net.spy.memcached.cas.CASMutator;
import net.spy.memcached.cas.CASValue;
import net.spy.memcached.transcoders.LongTranscoder;
import net.spy.test.SyncThread;

/**
 * Test the CAS mutator.
 */
public class CASMutatorTest extends ClientBaseCase {

	private CASMutation<Long> mutation;
	private CASMutator<Long> mutator;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mutation=new CASMutation<Long>() {
			public Long getNewValue(Long current) {
				return current+1;
			}
		};
		mutator=new CASMutator<Long>(client, new LongTranscoder(), 50);
	}

	public void testDefaultConstructor() {
		// Just validate that this doesn't throw an exception.
		new CASMutator<Long>(client, new LongTranscoder());
	}

	public void testConcurrentCAS() throws Throwable {
		int num=SyncThread.getDistinctResultCount(20, new Callable<Long>(){
			public Long call() throws Exception {
				return mutator.cas("test.cas.concurrent", 0L, 0, mutation);
			}});
		assertEquals(20, num);
	}

	public void testIncorrectTypeInCAS() throws Throwable {
		// Stick something for this CAS in the cache.
		client.set("x", 0, "not a long");
		try {
			Long rv=mutator.cas("x", 1L, 0, mutation);
			fail("Expected RuntimeException on invalid type mutation, got "
				+ rv);
		} catch(RuntimeException e) {
			assertEquals("Couldn't get a CAS in 50 attempts", e.getMessage());
		}
	}

	public void testCASValueToString() {
		CASValue<String> c=new CASValue<String>(717L, "hi");
		assertEquals("{CasValue 717/hi}", c.toString());
	}
}
