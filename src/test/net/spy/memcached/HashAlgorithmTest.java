package net.spy.memcached;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Test the hash algorithms.
 */
public class HashAlgorithmTest extends TestCase {

	private void assertHash(HashAlgorithm ha, String key, long exp) {
		assertTrue(exp >= 0L);
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
		Map<String, Long> exp=new HashMap<String, Long>();
		exp.put("Test1", 19315L);
		exp.put("Test2", 21114L);
		exp.put("Test3", 9597L);
		exp.put("Test4", 15129L);

		for(Map.Entry<String, Long> me : exp.entrySet()) {
			assertHash(HashAlgorithm.CRC32_HASH, me.getKey(), me.getValue());
		}
	}

	// Thanks much to pierre@demartines.com for this unit test.
	public void testFowlerNollVoHash() {
    	HashMap<String,Long> exp = new HashMap<String,Long>();
    	exp.put("", 0xcbf29ce484222325L);
    	exp.put(" ", 0xaf63bd4c8601b7ffL);
    	exp.put("hello world!", new Long(0x58735284b97b86bcL));
    	exp.put("Lorem ipsum dolor sit amet, consectetuer adipiscing elit.",
    		0x536c9cdee87c054aL);
    	exp.put("wd:com.google", 0xcf4e7986071b08f8L);
    	exp.put("wd:com.google ", 0x5d6176be12f03d48L);

		for(Map.Entry<String, Long> me : exp.entrySet()) {
			assertHash(HashAlgorithm.FNV_HASH, me.getKey(),
				Math.abs(me.getValue()));
		}

	}
}
