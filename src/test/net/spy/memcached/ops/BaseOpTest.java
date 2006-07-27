// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: AAE1AD0E-CDD6-4143-B607-C79DFEF7BCA2

package net.spy.memcached.ops;

import java.nio.ByteBuffer;
import java.util.Arrays;

import net.spy.test.BaseMockCase;

/**
 * Test the basic operation buffer handling stuff.
 */
public class BaseOpTest extends BaseMockCase {

	public void testAssertions() {
		try {
			assert false;
			fail("Assertions are not enabled.");
		} catch(AssertionError e) {
			// OK
		}
	}

	public void testDataReadType() throws Exception {
		SimpleOp op=new SimpleOp(Operation.ReadType.DATA);
		assertSame(Operation.ReadType.DATA, op.getReadType());
		// Make sure lines aren't handled
		try {
			op.handleLine("x");
			fail("Handled a line in data mode");
		} catch(AssertionError e) {
			// ok
		}
		op.handleRead(ByteBuffer.allocate(3));
	}

	public void testLineReadType() throws Exception {
		SimpleOp op=new SimpleOp(Operation.ReadType.LINE);
		assertSame(Operation.ReadType.LINE, op.getReadType());
		// Make sure lines aren't handled
		try {
			op.handleRead(ByteBuffer.allocate(3));
			fail("Handled data in line mode");
		} catch(AssertionError e) {
			// ok
		}
		op.handleLine("x");
	}

	// XXX: this broke when operation's buffer processing changed to drain the
	// buffer.  Only the operation knows enough to do the state changes and
	// inspection this thing is trying to do.
	public void xtestLineParser() throws Exception {
		String input="This is a multiline string\r\nhere is line two\r\nxyz";
		ByteBuffer b=ByteBuffer.wrap(input.getBytes());
		SimpleOp op=new SimpleOp(Operation.ReadType.LINE);
		op.readFromBuffer(b);
		assertEquals("This is a multiline string", op.getCurrentLine());
		op.readFromBuffer(b);
		assertEquals("here is line two", op.getCurrentLine());
		op.setReadType(Operation.ReadType.DATA);
		op.setBytesToRead(2);
		op.readFromBuffer(b);
		byte[] expected={'x', 'y'};
		assertTrue("Expected " + Arrays.toString(expected) + " but got "
				+ Arrays.toString(op.getCurentBytes()),
				Arrays.equals(expected, op.getCurentBytes()));
		assertEquals(1, b.remaining());
		assertEquals((byte)'z', b.get());
	}

	public void testPartialLine() throws Exception {
		String input1="this is a ";
		String input2="test\r\n";
		ByteBuffer b=ByteBuffer.allocate(20);
		SimpleOp op=new SimpleOp(Operation.ReadType.LINE);

		b.put(input1.getBytes());
		b.flip();
		op.readFromBuffer(b);
		assertNull(op.getCurrentLine());
		b.clear();
		b.put(input2.getBytes());
		b.flip();
		op.readFromBuffer(b);
		assertEquals("this is a test", op.getCurrentLine());
	}

	private static class SimpleOp extends Operation {

		private String currentLine=null;
		private byte[] currentBytes=null;
		private int bytesToRead=0;

		public SimpleOp(ReadType t) {
			setReadType(t);
		}

		public void setBytesToRead(int to) {
			bytesToRead=to;
		}

		public String getCurrentLine() {
			return currentLine;
		}

		public byte[] getCurentBytes() {
			return currentBytes;
		}

		@Override
		public void handleLine(String line) {
			assert getReadType() == Operation.ReadType.LINE;
			currentLine=line;
		}

		@Override
		public void handleRead(ByteBuffer data) {
			assert getReadType() == Operation.ReadType.DATA;
			if(bytesToRead > 0) {
				currentBytes=new byte[bytesToRead];
				data.get(currentBytes);
			}
		}

		@Override
		public void initialize() {
			setBuffer(ByteBuffer.allocate(0));
		}

		@Override
		protected void wasCancelled() {
			// nothing
		}
		
	}
}
