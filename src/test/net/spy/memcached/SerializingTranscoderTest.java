// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: A203E7D4-227B-4CB9-B286-C614A0AF397A

package net.spy.memcached;

import java.util.ArrayList;
import java.util.Arrays;
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
		Date d1=new Date();
		CachedData cd=tc.encode(d1);
		assertEquals(SerializingTranscoder.SERIALIZED, cd.getFlags());
		assertEquals(d1, tc.decode(cd));
	}

	public void testCompressedObject() throws Exception {
		tc.setCompressionThreshold(8);
		Date d1=new Date();
		CachedData cd=tc.encode(d1);
		assertEquals(SerializingTranscoder.SERIALIZED
				|SerializingTranscoder.COMPRESSED, cd.getFlags());
		assertEquals(d1, tc.decode(cd));
	}

	public void testSomethingBigger() throws Exception {
		Collection<Date> dates=new ArrayList<Date>();
		for(int i=0; i<1024; i++) {
			dates.add(new Date());
		}
		CachedData d=tc.encode(dates);
		assertEquals(dates, tc.decode(d));
	}
}
