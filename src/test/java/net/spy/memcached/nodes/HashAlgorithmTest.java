package net.spy.memcached.nodes;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import net.spy.memcached.util.KeyUtil;

/**
 * Test the hash algorithms.
 */
public class HashAlgorithmTest extends TestCase {

	private void assertHash(HashAlgorithm ha, byte[] key, long exp) {
		assertTrue(exp >= 0L);
		// System.out.println(ha + "(" + key + ") = " + exp);
		assertEquals("Invalid " + ha + " for key ``" + key + "''",
			exp, ha.hash(key));
	}

	// I don't hardcode any values here because they're subject to change
	private void assertNativeHash(String key) {
		// System.out.println(ha + "(" + key + ") = " + exp);
		assertHash(HashAlgorithm.NATIVE_HASH,
			KeyUtil.getKeyBytes(key), Math.abs(key.hashCode()));	}

	public void testNativeHash() {
		for (String k : new String[] { "Test1", "Test2", "Test3", "Test4" }) {
			assertNativeHash(k);
		}
	}

	public void testCrc32Hash() {
		Map<String, Long> exp = new HashMap<String, Long>();
		exp.put("Test1", 19315L);
		exp.put("Test2", 21114L);
		exp.put("Test3", 9597L);
		exp.put("Test4", 15129L);
		exp.put("UDATA:edevil@sapo.pt", 558L);

		for (Map.Entry<String, Long> me : exp.entrySet()) {
			assertHash(HashAlgorithm.CRC32_HASH,
				KeyUtil.getKeyBytes(me.getKey()), me.getValue());
		}
	}

	public void testFnv1_64() {
		HashMap<String, Long> exp = new HashMap<String, Long>();
		exp.put("", 0x84222325L);
		exp.put(" ", 0x8601b7ffL);
		exp.put("hello world!", 0xb97b86bcL);
		exp.put("Lorem ipsum dolor sit amet, consectetuer adipiscing elit.",
				0xe87c054aL);
		exp.put("wd:com.google", 0x071b08f8L);
		exp.put("wd:com.google ", 0x12f03d48L);

		for (Map.Entry<String, Long> me : exp.entrySet()) {
			assertHash(HashAlgorithm.FNV1_64_HASH,
				KeyUtil.getKeyBytes(me.getKey()),
				Math.abs(me.getValue()));
		}
	}

	// Thanks much to pierre@demartines.com for this unit test.
	public void testFnv1a_64() {
		HashMap<String, Long> exp = new HashMap<String, Long>();
		exp.put("", 0x84222325L);
		exp.put(" ", 0x8601817fL);
		exp.put("hello world!", 0xcd5a2672L);
		exp.put("Lorem ipsum dolor sit amet, consectetuer adipiscing elit.",
				0xbec309a8L);
		exp.put("wd:com.google", 0x097b3f26L);
		exp.put("wd:com.google ", 0x1c6c1732L);

		for (Map.Entry<String, Long> me : exp.entrySet()) {
			assertHash(HashAlgorithm.FNV1A_64_HASH,
				KeyUtil.getKeyBytes(me.getKey()),
				Math.abs(me.getValue()));
		}
	}

	public void testFnv1_32() {
		HashMap<String, Long> exp = new HashMap<String, Long>();
		exp.put("", 0x811c9dc5L);
		exp.put(" ", 0x050c5d3fL);
		exp.put("hello world!", 0x8a01b99cL);
		exp.put("Lorem ipsum dolor sit amet, consectetuer adipiscing elit.",
				0x9277524aL);
		exp.put("wd:com.google", 0x455e0df8L);
		exp.put("wd:com.google ", 0x2b0ffd48L);

		for (Map.Entry<String, Long> me : exp.entrySet()) {
			assertHash(HashAlgorithm.FNV1_32_HASH,
				KeyUtil.getKeyBytes(me.getKey()),
				Math.abs(me.getValue()));
		}
	}

	public void testFnv1a_32() {
		HashMap<String, Long> exp = new HashMap<String, Long>();
		exp.put("", 0x811c9dc5L);
		exp.put(" ", 0x250c8f7fL);
		exp.put("hello world!", 0xb034fff2L);
		exp.put("Lorem ipsum dolor sit amet, consectetuer adipiscing elit.",
				0xa9795ec8L);
		exp.put("wd:com.google", 0xaa90fcc6L);
		exp.put("wd:com.google ", 0x683e1e12L);

		for (Map.Entry<String, Long> me : exp.entrySet()) {
			assertHash(HashAlgorithm.FNV1A_32_HASH,
				KeyUtil.getKeyBytes(me.getKey()),
				Math.abs(me.getValue()));
		}
	}

	// These values came from libketama's test prog.
	public void testKetamaHash() {
		HashMap<String, Long> exp = new HashMap<String, Long>();
		exp.put("26", 3979113294L);
		exp.put("1404", 2065000984L);
		exp.put("4177", 1125759251L);
		exp.put("9315", 3302915307L);
		exp.put("14745", 2580083742L);
		exp.put("105106", 3986458246L);
		exp.put("355107", 3611074310L);

		for (Map.Entry<String, Long> me : exp.entrySet()) {
			assertHash(HashAlgorithm.KETAMA_HASH,
				KeyUtil.getKeyBytes(me.getKey()),
				Math.abs(me.getValue()));
		}
	}
}
