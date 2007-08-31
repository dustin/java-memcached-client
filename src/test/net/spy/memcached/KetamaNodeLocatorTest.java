package net.spy.memcached;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;

/**
 * Test ketama node location.
 */
public class KetamaNodeLocatorTest extends AbstractNodeLocationCase {

	@Override
	protected void setupNodes(int n) {
		super.setupNodes(n);

		for(int i=0; i<nodeMocks.length; i++) {
			nodeMocks[i].expects(atLeastOnce())
				.method("getSocketAddress")
				.will(returnValue(InetSocketAddress.createUnresolved(
						"127.0.0.1", 10000 + i)));
		}

		locator=new KetamaNodeLocator(Arrays.asList(nodes),
			HashAlgorithm.KETAMA_HASH);
	}

	public void testAll() throws Exception {
		setupNodes(4);

		Collection<MemcachedNode> all = locator.getAll();
		assertEquals(4, all.size());
		for(int i=0; i<4; i++) {
			assertTrue(all.contains(nodes[i]));
		}
	}

	public void testLookups() {
		setupNodes(4);
		assertSame(nodes[0], locator.getPrimary("dustin"));
		assertSame(nodes[2], locator.getPrimary("noelani"));
		assertSame(nodes[0], locator.getPrimary("some other key"));
	}

	public void testContinuumWrapping() {
		/*
		// This is the method by which I found something that would wrap
		String key="a";
		// maximum key found in the ketama continuum
		while(HashAlgorithm.KETAMA_HASH.hash(key) <= 4290126876L) {
			key=PwGen.getPass(8);
		}
		System.out.println("Found a key past the end of the continuum:  "
			+ key);
		*/

		setupNodes(4);
		assertSame(nodes[1], locator.getPrimary("7QHNPFVC"));
		assertSame(nodes[1], locator.getPrimary("N6H4245M"));
	}

	public void testClusterResizing() {
		setupNodes(4);
		assertSame(nodes[0], locator.getPrimary("dustin"));
		assertSame(nodes[2], locator.getPrimary("noelani"));
		assertSame(nodes[0], locator.getPrimary("some other key"));

		setupNodes(5);
		assertSame(nodes[0], locator.getPrimary("dustin"));
		assertSame(nodes[2], locator.getPrimary("noelani"));
		assertSame(nodes[4], locator.getPrimary("some other key"));
	}

	public void testSequence1() {
		setupNodes(4);
		assertSequence("dustin", 0, 2, 1, 1);
	}

	public void testSequence2() {
		setupNodes(4);
		assertSequence("noelani", 2, 1, 3, 0);
	}

	private void assertPosForKey(String k, int nid) {
		assertSame(nodes[nid], locator.getPrimary(k));
	}

	public void testLibKetamaCompat() {
		setupNodes(5);
		assertPosForKey("36", 2);
		assertPosForKey("10037", 3);
		assertPosForKey("22051", 1);
		assertPosForKey("49044", 4);
	}
}
