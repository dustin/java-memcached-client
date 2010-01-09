package net.spy.memcached;

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
		new BinaryConnectionFactory(5, 5,
				HashAlgorithm.FNV1_64_HASH);
	}

	public void testQueueSizes() {
		ConnectionFactory cf=new DefaultConnectionFactory(100, 1024);
		assertEquals(100, cf.createOperationQueue().remainingCapacity());
		assertEquals(Integer.MAX_VALUE,
				cf.createWriteOperationQueue().remainingCapacity());
		assertEquals(Integer.MAX_VALUE,
				cf.createReadOperationQueue().remainingCapacity());
	}
}
