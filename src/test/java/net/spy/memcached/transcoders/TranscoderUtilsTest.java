package net.spy.memcached.transcoders;

import junit.framework.TestCase;

/**
 * Some test coverage for transcoder utils.
 */
public class TranscoderUtilsTest extends TestCase {

	byte[] oversizeBytes=new byte[16];

	public void testBooleanOverflow() {
		try {
			boolean b=TranscoderUtils.decodeBoolean(oversizeBytes);
			fail("Got " + b + " expected assertion.");
		} catch(AssertionError e) {
			// pass
		}
	}

	public void testByteOverflow() {
		try {
			byte b=TranscoderUtils.decodeByte(oversizeBytes);
			fail("Got " + b + " expected assertion.");
		} catch(AssertionError e) {
			// pass
		}
	}

	public void testIntOverflow() {
		try {
			int b=TranscoderUtils.decodeInt(oversizeBytes);
			fail("Got " + b + " expected assertion.");
		} catch(AssertionError e) {
			// pass
		}
	}

	public void testLongOverflow() {
		try {
			long b=TranscoderUtils.decodeLong(oversizeBytes);
			fail("Got " + b + " expected assertion.");
		} catch(AssertionError e) {
			// pass
		}
	}

}
