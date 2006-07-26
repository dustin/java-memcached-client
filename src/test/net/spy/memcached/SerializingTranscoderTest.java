// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: A203E7D4-227B-4CB9-B286-C614A0AF397A

package net.spy.memcached;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import net.spy.test.BaseMockCase;

/**
 * Test the serializing transcoder.
 */
public class SerializingTranscoderTest extends BaseMockCase {

	private SerializingTranscoder tc=null;

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
		assertEquals(0, cd.getFlags());
		assertTrue(Arrays.equals(s1.getBytes(), cd.getData()));
		assertEquals(s1, tc.decode(cd));
	}

	public void testCompressedString() throws Exception {
		String s1="This is a test simple string that will be compressed.";
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
		byte[] encoded=tc.encodeLong(l);
		long decoded=tc.decodeLong(encoded);
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
		byte[] encoded=tc.encodeInt(i);
		int decoded=tc.decodeInt(encoded);
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
		assertTrue(tc.decodeBoolean(tc.encodeBoolean(true)));
		assertFalse(tc.decodeBoolean(tc.encodeBoolean(false)));
	}

	public void testByteArray() throws Exception {
		byte[] a={'a', 'b', 'c'};
		CachedData cd=tc.encode(a);
		assertTrue(Arrays.equals(a, cd.getData()));
		assertTrue(Arrays.equals(a, (byte[])tc.decode(cd)));
	}
}
