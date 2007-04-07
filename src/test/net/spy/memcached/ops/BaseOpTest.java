// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.ops;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
		op.setBytesToRead(2);
		op.handleRead(ByteBuffer.wrap("hi".getBytes()));
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

	public void testLineParser() throws Exception {
		String input="This is a multiline string\r\nhere is line two\r\n";
		ByteBuffer b=ByteBuffer.wrap(input.getBytes());
		SimpleOp op=new SimpleOp(Operation.ReadType.LINE);
		op.linesToRead=2;
		op.readFromBuffer(b);
		assertEquals("This is a multiline string", op.getLines().get(0));
		assertEquals("here is line two", op.getLines().get(1));
		op.setBytesToRead(2);
		op.readFromBuffer(ByteBuffer.wrap("xy".getBytes()));
		byte[] expected={'x', 'y'};
		assertTrue("Expected " + Arrays.toString(expected) + " but got "
				+ Arrays.toString(op.getCurentBytes()),
				Arrays.equals(expected, op.getCurentBytes()));
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

		private LinkedList<String> lines=new LinkedList<String>();
		private byte[] currentBytes=null;
		private int bytesToRead=0;
		public int linesToRead=1;

		public SimpleOp(ReadType t) {
			setReadType(t);
		}

		public void setBytesToRead(int to) {
			bytesToRead=to;
		}

		public String getCurrentLine() {
			return lines.isEmpty()?null:lines.getLast();
		}

		public List<String> getLines() {
			return lines;
		}

		public byte[] getCurentBytes() {
			return currentBytes;
		}

		@Override
		public void handleLine(String line) {
			assert getReadType() == Operation.ReadType.LINE;
			lines.add(line);
			if(--linesToRead == 0) {
				setReadType(Operation.ReadType.DATA);
			}
		}

		@Override
		public void handleRead(ByteBuffer data) {
			assert getReadType() == Operation.ReadType.DATA;
			assert bytesToRead > 0;
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
