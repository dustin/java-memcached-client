package net.spy.memcached;

import java.util.Collection;
import java.util.Iterator;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

/**
 * Test the ArrayModNodeLocator.
 */
public class ArrayModNodeLocatorTest extends MockObjectTestCase {

	// Test nodes.
	private MemcachedNode[] nodes;
	// Mocks for the above nodes.
	private Mock[] nodeMocks;

	private ArrayModNodeLocator locator;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		nodes=new MemcachedNode[4];
		nodeMocks=new Mock[nodes.length];

		for(int i=0; i<nodeMocks.length; i++) {
			nodeMocks[i]=mock(MemcachedNode.class, "node#" + i);
			nodes[i]=(MemcachedNode)nodeMocks[i].proxy();
		}

		locator=new ArrayModNodeLocator(nodes, HashAlgorithm.NATIVE_HASH);
	}

	public void testPrimary() throws Exception {
		assertSame(nodes[1], locator.getPrimary("dustin"));
		assertSame(nodes[0], locator.getPrimary("x"));
		assertSame(nodes[1], locator.getPrimary("y"));
	}

	public void testAll() throws Exception {
		Collection<MemcachedNode> all = locator.getAll();
		assertEquals(4, all.size());
		assertTrue(all.contains(nodes[0]));
		assertTrue(all.contains(nodes[1]));
		assertTrue(all.contains(nodes[2]));
		assertTrue(all.contains(nodes[3]));
	}

	private void assertSequence(String k, int... seq) {
		int pos=0;
		for(Iterator<MemcachedNode> i=locator.getSequence(k); i.hasNext(); ) {
			assertSame("At position " + pos, nodes[seq[pos]], i.next());
			pos++;
		}
	}

	public void testSeq1() {
		assertSequence("dustin", 2, 3, 0, 1);
		assertSequence("noelani", 1, 2, 3, 0);
	}
}
