package net.spy.memcached;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;

import net.spy.SpyObject;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OptimizedGet;

/**
 * Represents a node with the memcached cluster, along with buffering and
 * operation queues.
 */
class MemcachedNode extends SpyObject {
	public final SocketAddress socketAddress;
	public final ByteBuffer rbuf;
	public final ByteBuffer wbuf;
	private final BlockingQueue<Operation> writeQ;
	private final BlockingQueue<Operation> readQ;
	private final BlockingQueue<Operation> inputQueue;
	public final int which;
	// This has been declared volatile so it can be used as an availability
	// indicator.
	public volatile int reconnectAttempt=1;
	public SocketChannel channel;
	public int toWrite=0;
	private GetOperation getOp=null;
	public SelectionKey sk=null;

	// Count sequential protocol errors.
	public int protocolErrors=0;

	public MemcachedNode(int pos, SocketAddress sa, SocketChannel c,
			int bufSize, BlockingQueue<Operation> rq,
			BlockingQueue<Operation> wq, BlockingQueue<Operation> iq) {
		super();
		assert sa != null : "No SocketAddress";
		assert c != null : "No SocketChannel";
		assert bufSize > 0 : "Invalid buffer size: " + bufSize;
		assert rq != null : "No operation read queue";
		assert wq != null : "No operation write queue";
		assert iq != null : "No input queue";
		which=pos;
		socketAddress=sa;
		channel=c;
		rbuf=ByteBuffer.allocate(bufSize);
		wbuf=ByteBuffer.allocate(bufSize);
		wbuf.clear();
		readQ=rq;
		writeQ=wq;
		inputQueue=iq;
	}

	public void copyInputQueue() {
		Collection<Operation> tmp=new ArrayList<Operation>();
		inputQueue.drainTo(tmp);
		writeQ.addAll(tmp);
	}


	public void setupResend() {
		// First, reset the current write op.
		Operation op=getCurrentWriteOp();
		if(op != null) {
			op.getBuffer().reset();
		}
		// Now cancel all the pending read operations.  Might be better to
		// to requeue them.
		while(hasReadOp()) {
			op=removeCurrentReadOp();
			getLogger().warn("Discarding partially completed op: %s", op);
			op.cancel();
		}

		wbuf.clear();
		rbuf.clear();
		toWrite=0;
		protocolErrors=0;
	}

	// Prepare the pending operations.  Return true if there are any pending
	// ops
	private boolean preparePending() {
		// Copy the input queue into the write queue.
		copyInputQueue();

		// Now check the ops
		Operation nextOp=getCurrentWriteOp();
		while(nextOp != null && nextOp.isCancelled()) {
			getLogger().info("Removing cancelled operation: %s", nextOp);
			removeCurrentWriteOp();
			nextOp=getCurrentWriteOp();
		}
		return nextOp != null;
	}

	public void fillWriteBuffer(boolean optimizeGets) {
		if(toWrite == 0) {
			wbuf.clear();
			Operation o=getCurrentWriteOp();
			while(o != null && toWrite < wbuf.capacity()) {
				assert o.getState() == Operation.State.WRITING;
				ByteBuffer obuf=o.getBuffer();
				int bytesToCopy=Math.min(wbuf.remaining(),
						obuf.remaining());
				byte b[]=new byte[bytesToCopy];
				obuf.get(b);
				wbuf.put(b);
				getLogger().debug("After copying stuff from %s: %s",
						o, wbuf);
				if(!o.getBuffer().hasRemaining()) {
					o.writeComplete();
					transitionWriteItem();

					preparePending();
					if(optimizeGets) {
						optimize();
					}

					o=getCurrentWriteOp();
				}
				toWrite += bytesToCopy;
			}
			wbuf.flip();
			assert toWrite <= wbuf.capacity()
				: "toWrite exceeded capacity: " + this;
			assert toWrite == wbuf.remaining()
				: "Expected " + toWrite + " remaining, got "
				+ wbuf.remaining();
		} else {
			getLogger().debug("Buffer is full, skipping");
		}
	}

	public void transitionWriteItem() {
		Operation op=removeCurrentWriteOp();
		assert op != null : "There is no write item to transition";
		assert op.getState() == Operation.State.READING;
		getLogger().debug("Transitioning %s to read", op);
		readQ.add(op);
	}

	public void optimize() {
		// make sure there are at least two get operations in a row before
		// attempting to optimize them.
		if(writeQ.peek() instanceof GetOperation) {
			getOp=(GetOperation)writeQ.remove();
			if(writeQ.peek() instanceof GetOperation) {
				OptimizedGet og=new OptimizedGet(getOp);
				getOp=og;

				while(writeQ.peek() instanceof GetOperation) {
					GetOperation o=(GetOperation) writeQ.remove();
					if(!o.isCancelled()) {
						og.addOperation(o);
					}
				}

				// Initialize the new mega get
				getOp.initialize();
				assert getOp.getState() == Operation.State.WRITING;
				getLogger().debug(
					"Set up %s with %s keys and %s callbacks",
					this, og.numKeys(), og.numCallbacks());
			}
		}
	}

	public Operation getCurrentReadOp() {
		return readQ.peek();
	}

	public Operation removeCurrentReadOp() {
		return readQ.remove();
	}

	public Operation getCurrentWriteOp() {
		return getOp == null ? writeQ.peek() : getOp;
	}

	public Operation removeCurrentWriteOp() {
		Operation rv=getOp;
		if(rv == null) {
			rv=writeQ.remove();
		} else {
			getOp=null;
		}
		return rv;
	}

	public boolean hasReadOp() {
		return !readQ.isEmpty();
	}

	public boolean hasWriteOp() {
		return !(getOp == null && writeQ.isEmpty());
	}

	public void addOp(Operation op) {
		boolean added=inputQueue.add(op);
		assert added; // documented to throw an IllegalStateException
	}

	public int getSelectionOps() {
		int rv=0;
		if(channel.isConnected()) {
			if(hasReadOp()) {
				rv |= SelectionKey.OP_READ;
			}
			if(toWrite > 0 || hasWriteOp()) {
				rv |= SelectionKey.OP_WRITE;
			}
		} else {
			rv = SelectionKey.OP_CONNECT;
		}
		return rv;
	}

	@Override
	public String toString() {
		int sops=0;
		if(sk!= null && sk.isValid()) {
			sops=sk.interestOps();
		}
		int rsize=readQ.size() + (getOp == null ? 0 : 1);
		int wsize=writeQ.size();
		int isize=inputQueue.size();
		return "{QA sa=" + socketAddress + ", #Rops=" + rsize
			+ ", #Wops=" + wsize
			+ ", #iq=" + isize
			+ ", topRop=" + getCurrentReadOp()
			+ ", topWop=" + getCurrentWriteOp()
			+ ", toWrite=" + toWrite
			+ ", interested=" + sops + "}";
	}
}