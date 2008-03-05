// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.transcoders;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import net.spy.memcached.CachedData;

import net.spy.test.BaseMockCase;

/**
 * Test the serializing transcoder.
 */
public class SerializingTranscoderTest extends BaseMockCase {

	private SerializingTranscoder tc=null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tc=new SerializingTranscoder();
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
		String exp="{CachedData flags=0 data=[84, 104, 105, 115, 32, 105, "
			+ "115, 32, 97, 32, 115, 105, 109, 112, 108, 101, 32, 116, 101, "
			+ "115, 116, 32, 115, 116, 114, 105, 110, 103, 46]}";
		assertEquals(exp, String.valueOf(cd));
		assertEquals(0, cd.getFlags());
		assertTrue(Arrays.equals(s1.getBytes(), cd.getData()));
		assertEquals(s1, tc.decode(cd));
	}

	public void testUTF8String() throws Exception {
		String s1="\u2013\u00f3\u2013\u00a5\u2014\u00c4\u2013\u221e\u2013"
			+ "\u2264\u2014\u00c5\u2014\u00c7\u2013\u2264\u2014\u00c9\u2013"
			+ "\u03c0, \u2013\u00ba\u2013\u220f\u2014\u00c4.";
		CachedData cd=tc.encode(s1);
		// Test the stringification while we're here.
		String exp="{CachedData flags=0 data=[-30, -128, -109, -61, -77, -30, "
			+ "-128, -109, -62, -91, -30, -128, -108, -61, -124, -30, "
			+ "-128, -109, -30, -120, -98, -30, -128, -109, -30, -119, "
			+ "-92, -30, -128, -108, -61, -123, -30, -128, -108, -61, -121, "
			+ "-30, -128, -109, -30, -119, -92, -30, -128, -108, -61, -119, "
			+ "-30, -128, -109, -49, -128, 44, 32, -30, -128, -109, -62, -70, "
			+ "-30, -128, -109, -30, -120, -113, -30, -128, -108, -61, -124, "
			+ "46]}";
		assertEquals(exp, String.valueOf(cd));
		assertEquals(0, cd.getFlags());
		assertTrue(Arrays.equals(s1.getBytes("UTF-8"), cd.getData()));
		assertEquals(s1, tc.decode(cd));
	}

	public void testValidCharacterSet() {
		tc.setCharset("KOI8");
	}

	public void testInvalidCharacterSet() {
		try {
			tc.setCharset("Dustin's Kick Ass Character Set");
		} catch(RuntimeException e) {
			assertTrue(e.getCause() instanceof UnsupportedEncodingException);
		}
	}

	public void testCompressedStringNotSmaller() throws Exception {
		String s1="This is a test simple string that will not be compressed.";
		// Reduce the compression threshold so it'll attempt to compress it.
		tc.setCompressionThreshold(8);
		CachedData cd=tc.encode(s1);
		// This should *not* be compressed because it is too small
		assertEquals(0, cd.getFlags());
		assertTrue(Arrays.equals(s1.getBytes(), cd.getData()));
		assertEquals(s1, tc.decode(cd));
	}

	public void testCompressedString() throws Exception {
		// This one will actually compress
		String s1="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
		tc.setCompressionThreshold(8);
		CachedData cd=tc.encode(s1);
		assertEquals(SerializingTranscoder.COMPRESSED, cd.getFlags());
		assertFalse(Arrays.equals(s1.getBytes(), cd.getData()));
		assertEquals(s1, tc.decode(cd));
	}

	public void testObject() throws Exception {
		Calendar c=Calendar.getInstance();
		CachedData cd=tc.encode(c);
		assertEquals(SerializingTranscoder.SERIALIZED, cd.getFlags());
		assertEquals(c, tc.decode(cd));
	}

	public void testCompressedObject() throws Exception {
		tc.setCompressionThreshold(8);
		Calendar c=Calendar.getInstance();
		CachedData cd=tc.encode(c);
		assertEquals(SerializingTranscoder.SERIALIZED
				|SerializingTranscoder.COMPRESSED, cd.getFlags());
		assertEquals(c, tc.decode(cd));
	}

	public void testSomethingBigger() throws Exception {
		Collection<Date> dates=new ArrayList<Date>();
		for(int i=0; i<1024; i++) {
			dates.add(new Date());
		}
		CachedData d=tc.encode(dates);
		assertEquals(dates, tc.decode(d));
	}

	public void testDate() throws Exception {
		Date d=new Date();
		CachedData cd=tc.encode(d);
		assertEquals(d, tc.decode(cd));
	}

	public void testLong() throws Exception {
		assertEquals(923l, tc.decode(tc.encode(923l)));
	}

	public void testInt() throws Exception {
		assertEquals(923, tc.decode(tc.encode(923)));
	}

	public void testBoolean() throws Exception {
		assertSame(Boolean.TRUE, tc.decode(tc.encode(true)));
		assertSame(Boolean.FALSE, tc.decode(tc.encode(false)));
	}

	public void testByte() throws Exception {
		assertEquals((byte)-127, tc.decode(tc.encode((byte)-127)));
	}

	private void assertFloat(float f) {
		assertEquals(f, tc.decode(tc.encode(f)));
	}

	public void testFloat() throws Exception {
		assertFloat(0f);
		assertFloat(Float.MIN_VALUE);
		assertFloat(Float.MAX_VALUE);
		assertFloat(3.14f);
		assertFloat(-3.14f);
		assertFloat(Float.NaN);
		assertFloat(Float.POSITIVE_INFINITY);
		assertFloat(Float.NEGATIVE_INFINITY);
	}

	private void assertDouble(double d) {
		assertEquals(d, tc.decode(tc.encode(d)));
	}

	public void testDouble() throws Exception {
		assertDouble(0d);
		assertDouble(Double.MIN_VALUE);
		assertDouble(Double.MAX_VALUE);
		assertDouble(3.14d);
		assertDouble(-3.14d);
		assertDouble(Double.NaN);
		assertDouble(Double.POSITIVE_INFINITY);
		assertDouble(Double.NEGATIVE_INFINITY);
	}
	private void assertLong(long l) {
		byte[] encoded=TranscoderUtils.encodeLong(l);
		long decoded=TranscoderUtils.decodeLong(encoded);
		assertEquals(l, decoded);
	}

	/*
	private void displayBytes(long l, byte[] encoded) {
		System.out.print(l + " [");
		for(byte b : encoded) {
			System.out.print((b<0?256+b:b) + " ");
		}
		System.out.println("]");
	}
	*/

	public void testLongEncoding() throws Exception {
		assertLong(Long.MIN_VALUE);
		assertLong(1);
		assertLong(23852);
		assertLong(0l);
		assertLong(-1);
		assertLong(-23835);
		assertLong(Long.MAX_VALUE);
	}

	private void assertInt(int i) {
		byte[] encoded=TranscoderUtils.encodeInt(i);
		int decoded=TranscoderUtils.decodeInt(encoded);
		assertEquals(i, decoded);
	}

	public void testIntEncoding() throws Exception {
		assertInt(Integer.MIN_VALUE);
		assertInt(83526);
		assertInt(1);
		assertInt(0);
		assertInt(-1);
		assertInt(-238526);
		assertInt(Integer.MAX_VALUE);
	}

	public void testBooleanEncoding() throws Exception {
		assertTrue(TranscoderUtils.decodeBoolean(TranscoderUtils.encodeBoolean(true)));
		assertFalse(TranscoderUtils.decodeBoolean(TranscoderUtils.encodeBoolean(false)));
	}

	public void testByteArray() throws Exception {
		byte[] a={'a', 'b', 'c'};
		CachedData cd=tc.encode(a);
		assertTrue(Arrays.equals(a, cd.getData()));
		assertTrue(Arrays.equals(a, (byte[])tc.decode(cd)));
	}
}
