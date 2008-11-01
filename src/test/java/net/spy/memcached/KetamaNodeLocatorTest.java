package net.spy.memcached;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;

/**
 * Test ketama node location.
 */
public class KetamaNodeLocatorTest extends AbstractNodeLocationCase {

	protected void setupNodes(HashAlgorithm alg, int n) {
		super.setupNodes(n);
		for(int i=0; i<nodeMocks.length; i++) {
			nodeMocks[i].expects(atLeastOnce())
				.method("getSocketAddress")
				.will(returnValue(InetSocketAddress.createUnresolved(
						"127.0.0.1", 10000 + i)));
		}

		locator=new KetamaNodeLocator(Arrays.asList(nodes), alg);
	}

	@Override
	protected void setupNodes(int n) {
		setupNodes(HashAlgorithm.KETAMA_HASH, n);
	}

	public void testAll() throws Exception {
		setupNodes(4);

		Collection<MemcachedNode> all = locator.getAll();
		assertEquals(4, all.size());
		for(int i=0; i<4; i++) {
			assertTrue(all.contains(nodes[i]));
		}
	}

	public void testAllClone() throws Exception {
		setupNodes(4);

		Collection<MemcachedNode> all = locator.getReadonlyCopy().getAll();
		assertEquals(4, all.size());
	}

	public void testLookups() {
		setupNodes(4);
		assertSame(nodes[0], locator.getPrimary("dustin"));
		assertSame(nodes[2], locator.getPrimary("noelani"));
		assertSame(nodes[0], locator.getPrimary("some other key"));
	}

	public void testLookupsClone() {
		setupNodes(4);
		assertSame(nodes[0].toString(),
			locator.getReadonlyCopy().getPrimary("dustin").toString());
		assertSame(nodes[2].toString(),
			locator.getReadonlyCopy().getPrimary("noelani").toString());
		assertSame(nodes[0].toString(),
			locator.getReadonlyCopy().getPrimary("some other key").toString());
	}

	public void testContinuumWrapping() {
		setupNodes(4);
		// This is the method by which I found something that would wrap
		/*
		String key="a";
		// maximum key found in the ketama continuum
		long lastKey=((KetamaNodeLocator)locator).getMaxKey();
		while(HashAlgorithm.KETAMA_HASH.hash(key) < lastKey) {
			key=PwGen.getPass(8);
		}
		System.out.println("Found a key past the end of the continuum:  "
			+ key);
		*/
		assertEquals(4294887009L, ((KetamaNodeLocator)locator).getMaxKey());

		assertSame(nodes[3], locator.getPrimary("V5XS8C8N"));
		assertSame(nodes[3], locator.getPrimary("8KR2DKR2"));
		assertSame(nodes[3], locator.getPrimary("L9KH6X4X"));
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
		assertSequence("dustin", 0, 2, 1, 2);
	}

	public void testSequence2() {
		setupNodes(4);
		assertSequence("noelani", 2, 1, 1, 3);
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

	public void testFNV1A_32() {
		HashAlgorithm alg=HashAlgorithm.FNV1A_32_HASH;
		setupNodes(alg, 5);
		assertSequence("noelani", 1, 2, 2, 2, 3);

		assertSame(nodes[2], locator.getPrimary("dustin"));
		assertSame(nodes[1], locator.getPrimary("noelani"));
		assertSame(nodes[4], locator.getPrimary("some other key"));
	}
}
