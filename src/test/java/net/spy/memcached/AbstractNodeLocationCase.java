package net.spy.memcached;

import java.util.Iterator;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public abstract class AbstractNodeLocationCase extends MockObjectTestCase {

	protected MemcachedNode[] nodes;
	protected Mock[] nodeMocks;
	protected NodeLocator locator;

	private void runSequenceAssertion(NodeLocator l, String k, int... seq) {
		int pos=0;
		for(Iterator<MemcachedNode> i=l.getSequence(k); i.hasNext(); ) {
			assertEquals("At position " + pos, nodes[seq[pos]].toString(),
				i.next().toString());
			try {
				i.remove();
				fail("Allowed a removal from a sequence.");
			} catch(UnsupportedOperationException e) {
				// pass
			}
			pos++;
		}
		assertEquals("Incorrect sequence size for " + k, seq.length, pos);
	}

	public final void testCloningGetPrimary() {
		setupNodes(5);
		assertTrue(locator.getReadonlyCopy().getPrimary("hi")
			instanceof MemcachedNodeROImpl);
	}

	public final void testCloningGetAll() {
		setupNodes(5);
		assertTrue(locator.getReadonlyCopy().getAll().iterator().next()
			instanceof MemcachedNodeROImpl);
	}

	public final void testCloningGetSequence() {
		setupNodes(5);
		assertTrue(locator.getReadonlyCopy().getSequence("hi").next()
			instanceof MemcachedNodeROImpl);
	}

	protected final void assertSequence(String k, int... seq) {
		runSequenceAssertion(locator, k, seq);
		runSequenceAssertion(locator.getReadonlyCopy(), k, seq);
	}

	protected void setupNodes(int n) {
		nodes=new MemcachedNode[n];
		nodeMocks=new Mock[nodes.length];

		for(int i=0; i<nodeMocks.length; i++) {
			nodeMocks[i]=mock(MemcachedNode.class, "node#" + i);
			nodes[i]=(MemcachedNode)nodeMocks[i].proxy();
		}
	}
}