package net.spy.memcached.nodes;

import java.util.Arrays;
import java.util.Collection;

/**
 * Test the ArrayModNodeLocator.
 */
public class ArrayModNodeLocatorTest extends AbstractNodeLocationCase {

	@Override
	protected void setupNodes(int n) {
		super.setupNodes(n);
		locator=new ArrayModNodeLocator(Arrays.asList(nodes),
			HashAlgorithm.FNV1A_32_HASH);
	}

	public void testPrimary() throws Exception {
		setupNodes(4);
		assertSame(nodes[2], locator.getPrimary("dustin"));
		assertSame(nodes[3], locator.getPrimary("x"));
		assertSame(nodes[0], locator.getPrimary("y"));
	}

	public void testPrimaryClone() throws Exception {
		setupNodes(4);
		assertEquals(nodes[2].toString(),
			locator.getReadonlyCopy().getPrimary("dustin").toString());
		assertEquals(nodes[3].toString(),
			locator.getReadonlyCopy().getPrimary("x").toString());
		assertEquals(nodes[0].toString(),
			locator.getReadonlyCopy().getPrimary("y").toString());
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

	public void testAllClone() throws Exception {
		setupNodes(4);
		Collection<MemcachedNode> all = locator.getReadonlyCopy().getAll();
		assertEquals(4, all.size());
	}

	public void testSeq1() {
		setupNodes(4);
		assertSequence("dustin", 3, 0, 1);
	}

	public void testSeq2() {
		setupNodes(4);
		assertSequence("noelani", 0, 1, 2);
	}

	public void testSeqOnlyOneServer() {
		setupNodes(1);
		assertSequence("noelani");
	}

	public void testSeqWithTwoNodes() {
		setupNodes(2);
		assertSequence("dustin", 1);
	}
}
