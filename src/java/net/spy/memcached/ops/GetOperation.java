// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.ops;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Operation for retrieving data.
 */
public class GetOperation extends Operation {

	private Collection<String> keys=null;
	private String currentKey=null;
	private int currentFlags=0;
	private byte[] data=null;
	private int readOffset=0;
	// Character we're looking for after a data read
	private byte lookingFor='\0';

	private Callback cb=null;

	/**
	 * Construct an empty get operation.  Used for subclassing.
	 */
	protected GetOperation() {
		super();
	}

	public GetOperation(String key, Callback c) {
		super(c);
		keys=Collections.singleton(key);
		cb=c;
	}

	public GetOperation(Collection<String> k, Callback c) {
		super(c);
		keys=new HashSet<String>(k);
		cb=c;
	}

	/**
	 * Add some additional keys to fetch.
	 */
	protected void setKeys(Collection<String> to) {
		keys=to;
	}

	/**
	 * Get the keys this GetOperation is looking for.
	 */
	protected Collection<String> getKeys() {
		assert keys != null : "Null keys in " + this;
		return keys;
	}

	@Override
	protected void setCallback(OperationCallback to) {
		super.setCallback(to);
		cb=(Callback)to;
	}

	@Override
	public void handleLine(String line) {
		if(line.equals("END")) {
			getLogger().debug("Get complete!");
			cb.receivedStatus(line);
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
		// If we're not looking for termination, we're still looking for data
		if(lookingFor == '\0') {
			int toRead=data.length - readOffset;
			int available=b.remaining();
			toRead=Math.min(toRead, available);
			getLogger().debug("Reading %d bytes", toRead);
			b.get(data, readOffset, toRead);
			readOffset+=toRead;
		}
		// Transition us into a ``looking for \r\n'' kind of state if we've
		// read enough and are still in a data state.
		if(readOffset == data.length && lookingFor == '\0') {
			cb.gotData(currentKey, currentFlags, data);
			lookingFor='\r';
		}
		// If we're looking for an ending byte, let's go find it.
		if(lookingFor != '\0' && b.hasRemaining()) {
			do {
				byte tmp=b.get();
				assert tmp == lookingFor : "Expecting " + lookingFor + ", got "
					+ (char)tmp;
				switch(lookingFor) {
					case '\r': lookingFor='\n'; break;
					case '\n': lookingFor='\0'; break;
					default:
						assert false: "Looking for unexpected char: "
							+ (char)lookingFor;
				}
			} while(lookingFor != '\0' && b.hasRemaining());
			// Completed the read, reset stuff.
			if(lookingFor == '\0') {
				currentKey=null;
				data=null;
				readOffset=0;
				currentFlags=0;
				getLogger().debug("Setting read type back to line.");
				setReadType(ReadType.LINE);
			}
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
	}

	/**
	 * Operation callback for the get request.
	 */
	public interface Callback extends OperationCallback {
		/**
		 * Callback for each result from a get.
		 * 
		 * @param key the key that was retrieved
		 * @param flags the flags for this value
		 * @param data the data stored under this key
		 */
		void gotData(String key, int flags, byte[] data);
	}

	@Override
	protected void wasCancelled() {
		cb.receivedStatus("cancelled");
	}
}
