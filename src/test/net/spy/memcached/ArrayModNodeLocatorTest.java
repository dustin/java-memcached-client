package net.spy.memcached;

import java.util.Arrays;
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

	private void setupNodes(int n) {
		nodes=new MemcachedNode[n];
		nodeMocks=new Mock[nodes.length];

		for(int i=0; i<nodeMocks.length; i++) {
			nodeMocks[i]=mock(MemcachedNode.class, "node#" + i);
			nodes[i]=(MemcachedNode)nodeMocks[i].proxy();
		}

		locator=new ArrayModNodeLocator(Arrays.asList(nodes),
			HashAlgorithm.NATIVE_HASH);
	}

	public void testPrimary() throws Exception {
		setupNodes(4);
		assertSame(nodes[1], locator.getPrimary("dustin"));
		assertSame(nodes[0], locator.getPrimary("x"));
		assertSame(nodes[1], locator.getPrimary("y"));
	}

	public void testAll() throws Exception {
		setupNodes(4);
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
		assertEquals("Incorrect sequence size for " + k, seq.length, pos);
	}

	public void testSeq1() {
		setupNodes(4);
		assertSequence("dustin", 2, 3, 0);
	}

	public void testSeq2() {
		setupNodes(4);
		assertSequence("noelani", 1, 2, 3);
	}

	public void testSeqOnlyOneServer() {
		setupNodes(1);
		assertSequence("noelani");
	}

	public void testSeqWithTwoNodes() {
		setupNodes(2);
		assertSequence("dustin", 0);
	}
}
