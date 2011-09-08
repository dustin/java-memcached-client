package net.spy.memcached;

import junit.framework.TestCase;

/**
 * These tests test to make sure that we don't get null pointer
 * exceptions when calling toString methods on classes that have
 * custom toString() functions.
 */
public class ToStringTest extends TestCase {

	public void testDefaultConnectionFactory() {
		(new DefaultConnectionFactory()).toString();
		(new DefaultConnectionFactory(10, 1000)).toString();
		(new DefaultConnectionFactory(100, 100,
				HashAlgorithm.KETAMA_HASH)).toString();
	}

	public void testBinaryConnectionFactory() {
		(new BinaryConnectionFactory()).toString();
		(new BinaryConnectionFactory(10, 1000)).toString();
		(new BinaryConnectionFactory(100, 1000,
				HashAlgorithm.KETAMA_HASH)).toString();
	}
}
