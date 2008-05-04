package net.spy.memcached;

import net.spy.memcached.nodes.HashAlgorithm;

import junit.framework.TestCase;

/**
 * Test connection factory variations.
 */
public class ConnectionFactoryTest extends TestCase {

	// These tests are a little lame.  They don't verify anything other than
	// that the code executes without failure.
	public void testBinaryEmptyCons() {
		new BinaryConnectionFactory();
	}

	public void testBinaryTwoIntCons() {
		new BinaryConnectionFactory(5, 5);
	}

	public void testBinaryAnIntAnotherIntAndAHashAlgorithmCons() {
		new BinaryConnectionFactory(5, 5, HashAlgorithm.FNV1_64_HASH);
	}

	public void testQueueSizes() {
		ConnectionFactory cf=new DefaultConnectionFactory(100, 1024);
		assertEquals(100, cf.createOperationQueue().remainingCapacity());
		assertEquals(100, cf.createWriteOperationQueue().remainingCapacity());
		assertEquals(110, cf.createReadOperationQueue().remainingCapacity());
	}
}
