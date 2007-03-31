package net.spy.memcached;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Test the hash algorithms.
 */
public class HashAlgorithmTest extends TestCase {

	private void assertHash(HashAlgorithm ha, String key, int exp) {
		assertTrue(exp >= 0);
		assertEquals("Invalid " + ha + " for key " + key, exp, ha.hash(key));
		// System.out.println(ha + "(" + key + ") = " + exp);
	}

	// I don't hardcode any values here because they're subject to change
	private void assertNativeHash(String key) {
		assertHash(HashAlgorithm.NATIVE_HASH, key, Math.abs(key.hashCode())); 
	}

	public void testNativeHash() {
		for(String k : new String[]{"Test1", "Test2", "Test3", "Test4"}) {
			assertNativeHash(k);
		}
	}

	public void testCrc32Hash() {
		Map<String, Integer> exp=new HashMap<String, Integer>();
		exp.put("Test1", 19315);
		exp.put("Test2", 21114);
		exp.put("Test3", 9597);
		exp.put("Test4", 15129);

		for(Map.Entry<String, Integer> me : exp.entrySet()) {
			assertHash(HashAlgorithm.CRC32_HASH, me.getKey(), me.getValue());
		}
	}
}
