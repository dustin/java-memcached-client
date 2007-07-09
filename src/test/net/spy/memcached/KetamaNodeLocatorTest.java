package net.spy.memcached;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Iterator;

import junit.framework.AssertionFailedError;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.Invocation;
import org.jmock.core.InvocationMatcher;

/**
 * Test ketama node location.
 */
public class KetamaNodeLocatorTest extends MockObjectTestCase {

	// Test nodes.
	private MemcachedNode[] nodes;
	// Mocks for the above nodes.
	private Mock[] nodeMocks;

	private NodeLocator locator;

	private void setupNodes(int n) {
		nodes=new MemcachedNode[n];
		nodeMocks=new Mock[nodes.length];

		for(int i=0; i<nodeMocks.length; i++) {
			nodeMocks[i]=mock(MemcachedNode.class, "node#" + i);
			nodeMocks[i].expects(exactly(KetamaNodeLocator.NUM_REPS))
				.method("getSocketAddress")
				.will(returnValue(
					InetSocketAddress.createUnresolved(
							"127.0.0.1", 10000 + i)));
			nodes[i]=(MemcachedNode)nodeMocks[i].proxy();
		}

		locator=new KetamaNodeLocator(Arrays.asList(nodes),
			HashAlgorithm.KETAMA_HASH);
	}

	private InvocationMatcher exactly(final int n) {
        return new InvocationMatcher() {
            private int cnt = 0;

            public boolean matches(Invocation arg0) {
                return (cnt++ < n);
            }

            public void invoked(Invocation arg0) {
                // do nothing
            }

            public boolean hasDescription() {
                return true;
            }

            public void verify() {
                if (cnt != n) {
                    throw new AssertionFailedError("Expected " + n
                            + "invocations.  Got " + cnt);
                }
            }

            public StringBuffer describeTo(StringBuffer buf) {
                buf.append("allowed");
                return buf;
            }
        };
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

	private void assertSequence(String k, int... seq) {
		int pos=0;
		for(Iterator<MemcachedNode> i=locator.getSequence(k); i.hasNext(); ) {
			assertSame("At position " + pos, nodes[seq[pos]], i.next());
			pos++;
		}
		assertEquals("Incorrect sequence size for " + k, seq.length, pos);
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
