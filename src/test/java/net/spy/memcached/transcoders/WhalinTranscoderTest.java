// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.transcoders;

import java.util.Arrays;
import java.util.Calendar;

import net.spy.memcached.CachedData;

/**
 * Test the serializing transcoder.
 */
public class WhalinTranscoderTest extends BaseTranscoderCase {

	private WhalinTranscoder tc;
	private TranscoderUtils tu;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tc=new WhalinTranscoder();
		setTranscoder(tc);
		tu=new TranscoderUtils(false);
	}

	public void testNonserializable() throws Exception {
		try {
			tc.encode(new Object());
			fail("Processed a non-serializable object.");
		} catch(IllegalArgumentException e) {
			// pass
		}
	}

	public void testStrings() throws Exception {
		String s1="This is a simple test string.";
		CachedData cd=tc.encode(s1);
		// Test the stringification while we're here.
		String exp="{CachedData flags=32 data=[84, 104, 105, 115, 32, 105, "
			+ "115, 32, 97, 32, 115, 105, 109, 112, 108, 101, 32, 116, 101, "
			+ "115, 116, 32, 115, 116, 114, 105, 110, 103, 46]}";
		assertEquals(exp, String.valueOf(cd));
		assertEquals(WhalinTranscoder.SPECIAL_STRING, cd.getFlags());
		assertTrue(Arrays.equals(s1.getBytes(), cd.getData()));
		assertEquals(s1, tc.decode(cd));
	}

	public void testUTF8String() throws Exception {
		String s1="\u2013\u00f3\u2013\u00a5\u2014\u00c4\u2013\u221e\u2013"
			+ "\u2264\u2014\u00c5\u2014\u00c7\u2013\u2264\u2014\u00c9\u2013"
			+ "\u03c0, \u2013\u00ba\u2013\u220f\u2014\u00c4.";
		CachedData cd=tc.encode(s1);
		// Test the stringification while we're here.
		String exp="{CachedData flags=32 data=[-30, -128, -109, -61, -77, -30, "
			+ "-128, -109, -62, -91, -30, -128, -108, -61, -124, -30, "
			+ "-128, -109, -30, -120, -98, -30, -128, -109, -30, -119, "
			+ "-92, -30, -128, -108, -61, -123, -30, -128, -108, -61, -121, "
			+ "-30, -128, -109, -30, -119, -92, -30, -128, -108, -61, -119, "
			+ "-30, -128, -109, -49, -128, 44, 32, -30, -128, -109, -62, -70, "
			+ "-30, -128, -109, -30, -120, -113, -30, -128, -108, -61, -124, "
			+ "46]}";
		assertEquals(exp, String.valueOf(cd));
		assertEquals(WhalinTranscoder.SPECIAL_STRING, cd.getFlags());
		assertTrue(Arrays.equals(s1.getBytes("UTF-8"), cd.getData()));
		assertEquals(s1, tc.decode(cd));
	}

	public void testCompressedStringNotSmaller() throws Exception {
		String s1="This is a test simple string that will not be compressed.";
		// Reduce the compression threshold so it'll attempt to compress it.
		tc.setCompressionThreshold(8);
		CachedData cd=tc.encode(s1);
		// This should *not* be compressed because it is too small
		assertEquals(WhalinTranscoder.SPECIAL_STRING, cd.getFlags());
		assertTrue(Arrays.equals(s1.getBytes(), cd.getData()));
		assertEquals(s1, tc.decode(cd));
	}

	public void testCompressedString() throws Exception {
		// This one will actually compress
		String s1="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
		tc.setCompressionThreshold(8);
		CachedData cd=tc.encode(s1);
		assertEquals(
			WhalinTranscoder.COMPRESSED | WhalinTranscoder.SPECIAL_STRING,
			cd.getFlags());
		assertFalse(Arrays.equals(s1.getBytes(), cd.getData()));
		assertEquals(s1, tc.decode(cd));
	}

	public void testObject() throws Exception {
		Calendar c=Calendar.getInstance();
		CachedData cd=tc.encode(c);
		assertEquals(WhalinTranscoder.SERIALIZED, cd.getFlags());
		assertEquals(c, tc.decode(cd));
	}

	public void testCompressedObject() throws Exception {
		tc.setCompressionThreshold(8);
		Calendar c=Calendar.getInstance();
		CachedData cd=tc.encode(c);
		assertEquals(WhalinTranscoder.SERIALIZED
				|WhalinTranscoder.COMPRESSED, cd.getFlags());
		assertEquals(c, tc.decode(cd));
	}

	public void testUnencodeable() throws Exception {
		try {
			CachedData cd=tc.encode(new Object());
			fail("Should fail to serialize, got" + cd);
		} catch(IllegalArgumentException e) {
			// pass
		}
	}

	public void testUndecodeable() throws Exception {
		CachedData cd=new CachedData(
				Integer.MAX_VALUE &
				~(WhalinTranscoder.COMPRESSED | WhalinTranscoder.SERIALIZED),
				tu.encodeInt(Integer.MAX_VALUE));
		assertNull(tc.decode(cd));
	}

	public void testUndecodeableSerialized() throws Exception {
		CachedData cd=new CachedData(WhalinTranscoder.SERIALIZED,
				tu.encodeInt(Integer.MAX_VALUE));
		assertNull(tc.decode(cd));
	}

	public void testUndecodeableCompressed() throws Exception {
		CachedData cd=new CachedData(WhalinTranscoder.COMPRESSED,
				tu.encodeInt(Integer.MAX_VALUE));
		assertNull(tc.decode(cd));
	}

}
