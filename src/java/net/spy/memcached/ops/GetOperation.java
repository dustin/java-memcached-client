// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: BF4AFE32-9321-4557-8562-AA617D263BF5

package net.spy.memcached.ops;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;

/**
 * Operation for retrieving data.
 */
public class GetOperation extends Operation {

	private Collection<String> keys=null;
	private String currentKey=null;
	private int currentFlags=0;
	private byte[] data=null;
	private int readOffset=0;

	private Callback cb=null;

	public GetOperation(String key, Callback c) {
		this(Collections.singleton(key), c);
	}

	public GetOperation(Collection<String> k, Callback c) {
		super();
		keys=k;
		cb=c;
	}

	@Override
	public void handleLine(String line) {
		if(line.equals("END")) {
			getLogger().debug("Get complete!");
			if(cb != null) {
				cb.getComplete();
			}
			transitionState(State.COMPLETE);
			data=null;
		} else if(line.startsWith("VALUE ")) {
			getLogger().debug("Got line %s", line);
			String[] stuff=line.split(" ");
			assert stuff[0].equals("VALUE");
			currentKey=stuff[1];
			currentFlags=Integer.parseInt(stuff[2]);
			data=new byte[Integer.parseInt(stuff[3])];
			readOffset=0;
			getLogger().debug("Set read type to data");
			setReadType(ReadType.DATA);
		} else {
			assert false : "Unknown line type: " + line;
		}
	}

	@Override
	public void handleRead(ByteBuffer b) {
		assert currentKey != null;
		assert data != null;
		// This will be the case, because we'll clear them when it's not.
		assert readOffset <= data.length
			: "readOffset is " + readOffset + " data.length is " + data.length;

		getLogger().debug("readOffset: %d, length: %d",
				readOffset, data.length);
		int toRead=data.length - readOffset;
		int available=b.remaining();
		toRead=Math.min(toRead, available);
		getLogger().debug("Reading %d bytes", toRead);
		b.get(data, readOffset, toRead);
		readOffset+=toRead;
		if(readOffset == data.length) {
			if(cb != null) {
				cb.gotData(currentKey, currentFlags, data);
			}
			byte tmp=b.get();
			assert tmp == (byte)'\r' : " expected \\r, got " + (char)tmp;
			tmp=b.get();
			assert tmp == (byte)'\n' : " expected \\n, got " + (char)tmp;
			currentKey=null;
			data=null;
			readOffset=0;
			currentFlags=0;
			getLogger().debug("Setting read type back to line.");
			setReadType(ReadType.LINE);
		}
	}

	@Override
	public void initialize() {
		// Figure out the length of the request
		int size="get\r\n".length();
		for(String s : keys) {
			size+=s.length();
			size++;
		}
		ByteBuffer b=ByteBuffer.allocate(size);
		b.put("get".getBytes());
		for(String s : keys) {
			b.put((byte)' ');
			b.put(s.getBytes());
		}
		b.put("\r\n".getBytes());
		b.flip();
		setBuffer(b);
		keys=null;
	}

	public interface Callback {
		void gotData(String key, int flags, byte[] data);
		void getComplete();
	}
}
