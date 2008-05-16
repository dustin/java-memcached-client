package net.spy.memcached;

import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * A very basic test that the KetamaConnectionFactory returns both the correct
 * hash algorithm and the correct node locator.
 */
public class KetamaConnectionFactoryTest extends TestCase {

	/*
	 * This *is* kinda lame, but it tests the specific differences from the
	 * DefaultConnectionFactory.
	 */
	public void testCorrectTypes() {
		ConnectionFactory factory = new KetamaConnectionFactory();

		NodeLocator locator = factory.createLocator(new ArrayList<MemcachedNode>());
		assertTrue(locator instanceof KetamaNodeLocator);

		DefaultConnectionFactory dflt = (DefaultConnectionFactory) factory;
		assertEquals(HashAlgorithm.KETAMA_HASH, dflt.getHashAlg());
	}
}
