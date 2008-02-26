package net.spy.memcached;

import java.util.concurrent.Callable;

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
		mutator=new CASMutator<Long>(client);
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
			fail("Expected ClassCastException on invalid type mutation, got "
				+ rv);
		} catch(ClassCastException e) {
			// pass
		}
	}
}
