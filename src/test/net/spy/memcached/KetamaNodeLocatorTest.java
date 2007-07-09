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
			nodeMocks[i].expects(once())
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
		assertSame(nodes[1], locator.getPrimary("some other key"));
	}

	public void testClusterResizing() {
		setupNodes(4);
		assertSame(nodes[0], locator.getPrimary("dustin"));
		assertSame(nodes[2], locator.getPrimary("noelani"));
		assertSame(nodes[1], locator.getPrimary("some other key"));

		setupNodes(5);
		assertSame(nodes[0], locator.getPrimary("dustin"));
		assertSame(nodes[2], locator.getPrimary("noelani"));
		assertSame(nodes[1], locator.getPrimary("some other key"));
	}

	public void testSequence1() {
		setupNodes(4);
		assertSequence("dustin", 0, 1, 1, 3);
	}

	public void testSequence2() {
		setupNodes(4);
		assertSequence("noelani", 2, 0, 0, 0);
	}
}
