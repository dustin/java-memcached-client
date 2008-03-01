package net.spy.memcached.protocol.ascii;

import java.nio.ByteBuffer;
import java.util.Collection;

import net.spy.memcached.KeyUtil;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.GetsOperation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;

/**
 * Base class for get and gets handlers.
 */
abstract class BaseGetOpImpl extends OperationImpl {

	private static final OperationStatus END = new OperationStatus(true, "END");
	private final String cmd;
	private final Collection<String> keys;
	private String currentKey = null;
	private long casValue=0;
	private int currentFlags = 0;
	private byte[] data = null;
	private int readOffset = 0;
	private byte lookingFor = '\0';

	public BaseGetOpImpl(String c,
			OperationCallback cb, Collection<String> k) {
		super(cb);
		cmd=c;
		keys=k;
	}

	/**
	 * Get the keys this GetOperation is looking for.
	 */
	public final Collection<String> getKeys() {
		return keys;
	}

	@Override
	public final void handleLine(String line) {
		if(line.equals("END")) {
			getLogger().debug("Get complete!");
			getCallback().receivedStatus(END);
			transitionState(OperationState.COMPLETE);
			data=null;
		} else if(line.startsWith("VALUE ")) {
			getLogger().debug("Got line %s", line);
			String[] stuff=line.split(" ");
			assert stuff[0].equals("VALUE");
			currentKey=stuff[1];
			currentFlags=Integer.parseInt(stuff[2]);
			data=new byte[Integer.parseInt(stuff[3])];
			if(stuff.length > 4) {
				casValue=Long.parseLong(stuff[4]);
			}
			readOffset=0;
			getLogger().debug("Set read type to data");
			setReadType(OperationReadType.DATA);
		} else {
			assert false : "Unknown line type: " + line;
		}
	}

	@Override
	public final void handleRead(ByteBuffer b) {
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
			// The callback is most likely a get callback.  If it's not, then
			// it's a gets callback.
			try {
				GetOperation.Callback gcb=(GetOperation.Callback)getCallback();
				gcb.gotData(currentKey, currentFlags, data);
			} catch(ClassCastException e) {
				GetsOperation.Callback gcb=(GetsOperation.Callback)
					getCallback();
				gcb.gotData(currentKey, currentFlags, casValue, data);
			}
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
				setReadType(OperationReadType.LINE);
			}
		}
	}

	@Override
	public final void initialize() {
		// Figure out the length of the request
		int size=6; // Enough for gets\r\n
		Collection<byte[]> keyBytes=KeyUtil.getKeyBytes(keys);
		for(byte[] k : keyBytes) {
			size+=k.length;
			size++;
		}
		ByteBuffer b=ByteBuffer.allocate(size);
		b.put(cmd.getBytes());
		for(byte[] k : keyBytes) {
			b.put((byte)' ');
			b.put(k);
		}
		b.put("\r\n".getBytes());
		b.flip();
		setBuffer(b);
	}

	@Override
	protected final void wasCancelled() {
		getCallback().receivedStatus(CANCELLED);
	}

}
