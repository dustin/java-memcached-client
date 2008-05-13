package net.spy.memcached.protocol;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;

import net.spy.SpyObject;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationState;

/**
 * Represents a node with the memcached cluster, along with buffering and
 * operation queues.
 */
public abstract class TCPMemcachedNodeImpl extends SpyObject
	implements MemcachedNode {

	private final SocketAddress socketAddress;
	private final ByteBuffer rbuf;
	private final ByteBuffer wbuf;
	protected final BlockingQueue<Operation> writeQ;
	private final BlockingQueue<Operation> readQ;
	private final BlockingQueue<Operation> inputQueue;
	// This has been declared volatile so it can be used as an availability
	// indicator.
	private volatile int reconnectAttempt=1;
	private SocketChannel channel;
	private int toWrite=0;
	protected GetOperation getOp=null;
	private SelectionKey sk=null;

	public TCPMemcachedNodeImpl(SocketAddress sa, SocketChannel c,
			int bufSize, BlockingQueue<Operation> rq,
			BlockingQueue<Operation> wq, BlockingQueue<Operation> iq) {
		super();
		assert sa != null : "No SocketAddress";
		assert c != null : "No SocketChannel";
		assert bufSize > 0 : "Invalid buffer size: " + bufSize;
		assert rq != null : "No operation read queue";
		assert wq != null : "No operation write queue";
		assert iq != null : "No input queue";
		socketAddress=sa;
		setChannel(c);
		rbuf=ByteBuffer.allocate(bufSize);
		wbuf=ByteBuffer.allocate(bufSize);
		getWbuf().clear();
		readQ=rq;
		writeQ=wq;
		inputQueue=iq;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#copyInputQueue()
	 */
	public final void copyInputQueue() {
		Collection<Operation> tmp=new ArrayList<Operation>();

		// don't drain more than we have space to place
		inputQueue.drainTo(tmp, writeQ.remainingCapacity());

		writeQ.addAll(tmp);
	}


	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#setupResend()
	 */
	public final void setupResend() {
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

		getWbuf().clear();
		getRbuf().clear();
		toWrite=0;
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

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#fillWriteBuffer(boolean)
	 */
	public final void fillWriteBuffer(boolean optimizeGets) {
		if(toWrite == 0 && readQ.remainingCapacity() > 0) {
			getWbuf().clear();
			Operation o=getCurrentWriteOp();
			while(o != null && toWrite < getWbuf().capacity()) {
				assert o.getState() == OperationState.WRITING;
				ByteBuffer obuf=o.getBuffer();
				int bytesToCopy=Math.min(getWbuf().remaining(),
						obuf.remaining());
				byte b[]=new byte[bytesToCopy];
				obuf.get(b);
				getWbuf().put(b);
				getLogger().debug("After copying stuff from %s: %s",
						o, getWbuf());
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
			getWbuf().flip();
			assert toWrite <= getWbuf().capacity()
				: "toWrite exceeded capacity: " + this;
			assert toWrite == getWbuf().remaining()
				: "Expected " + toWrite + " remaining, got "
				+ getWbuf().remaining();
		} else {
			getLogger().debug("Buffer is full, skipping");
		}
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#transitionWriteItem()
	 */
	public final void transitionWriteItem() {
		Operation op=removeCurrentWriteOp();
		assert op != null : "There is no write item to transition";
		getLogger().debug("Transitioning %s to read", op);
		readQ.add(op);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#optimize()
	 */
	protected abstract void optimize();

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#getCurrentReadOp()
	 */
	public final Operation getCurrentReadOp() {
		return readQ.peek();
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#removeCurrentReadOp()
	 */
	public final Operation removeCurrentReadOp() {
		return readQ.remove();
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#getCurrentWriteOp()
	 */
	public final Operation getCurrentWriteOp() {
		return getOp == null ? writeQ.peek() : getOp;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#removeCurrentWriteOp()
	 */
	public final Operation removeCurrentWriteOp() {
		Operation rv=getOp;
		if(rv == null) {
			rv=writeQ.remove();
		} else {
			getOp=null;
		}
		return rv;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#hasReadOp()
	 */
	public final boolean hasReadOp() {
		return !readQ.isEmpty();
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#hasWriteOp()
	 */
	public final boolean hasWriteOp() {
		return !(getOp == null && writeQ.isEmpty());
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#addOp(net.spy.memcached.ops.Operation)
	 */
	public final void addOp(Operation op) {
		boolean added=inputQueue.add(op);
		assert added; // documented to throw an IllegalStateException
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#getSelectionOps()
	 */
	public final int getSelectionOps() {
		int rv=0;
		if(getChannel().isConnected()) {
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

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#getRbuf()
	 */
	public final ByteBuffer getRbuf() {
		return rbuf;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#getWbuf()
	 */
	public final ByteBuffer getWbuf() {
		return wbuf;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#getSocketAddress()
	 */
	public final SocketAddress getSocketAddress() {
		return socketAddress;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#isActive()
	 */
	public final boolean isActive() {
		return reconnectAttempt == 0
			&& getChannel() != null && getChannel().isConnected();
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#reconnecting()
	 */
	public final void reconnecting() {
		reconnectAttempt++;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#connected()
	 */
	public final void connected() {
		reconnectAttempt=0;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#getReconnectCount()
	 */
	public final int getReconnectCount() {
		return reconnectAttempt;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#toString()
	 */
	@Override
	public final String toString() {
		int sops=0;
		if(getSk()!= null && getSk().isValid()) {
			sops=getSk().interestOps();
		}
		int rsize=readQ.size() + (getOp == null ? 0 : 1);
		int wsize=writeQ.size();
		int isize=inputQueue.size();
		return "{QA sa=" + getSocketAddress() + ", #Rops=" + rsize
			+ ", #Wops=" + wsize
			+ ", #iq=" + isize
			+ ", topRop=" + getCurrentReadOp()
			+ ", topWop=" + getCurrentWriteOp()
			+ ", toWrite=" + toWrite
			+ ", interested=" + sops + "}";
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#registerChannel(java.nio.channels.SocketChannel, java.nio.channels.SelectionKey)
	 */
	public final void registerChannel(SocketChannel ch, SelectionKey skey) {
		setChannel(ch);
		setSk(skey);
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#setChannel(java.nio.channels.SocketChannel)
	 */
	public final void setChannel(SocketChannel to) {
		assert channel == null || !channel.isOpen()
			: "Attempting to overwrite channel";
		channel = to;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#getChannel()
	 */
	public final SocketChannel getChannel() {
		return channel;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#setSk(java.nio.channels.SelectionKey)
	 */
	public final void setSk(SelectionKey to) {
		sk = to;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#getSk()
	 */
	public final SelectionKey getSk() {
		return sk;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#getBytesRemainingInBuffer()
	 */
	public final int getBytesRemainingToWrite() {
		return toWrite;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.MemcachedNode#writeSome()
	 */
	public final int writeSome() throws IOException {
		int wrote=channel.write(wbuf);
		assert wrote >= 0 : "Wrote negative bytes?";
		toWrite -= wrote;
		assert toWrite >= 0
			: "toWrite went negative after writing " + wrote
				+ " bytes for " + this;
		getLogger().debug("Wrote %d bytes", wrote);
		return wrote;
	}


	public final void fixupOps() {
		if(sk != null && sk.isValid()) {
			int iops=getSelectionOps();
			getLogger().debug("Setting interested opts to %d", iops);
			sk.interestOps(iops);
		} else {
			getLogger().debug("Selection key is not valid.");
		}
	}
}
