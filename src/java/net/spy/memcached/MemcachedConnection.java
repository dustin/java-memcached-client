// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 30573332-B549-4E6F-AD59-04C6D0928419

package net.spy.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.spy.SpyObject;
import net.spy.memcached.ops.Operation;

/**
 * Connection to a cluster of memcached servers.
 */
public class MemcachedConnection extends SpyObject {
	// The number of empty selects we'll allow before taking action.  It's too
	// easy to write a bug that causes it to loop uncontrollably.  This helps
	// find those bugs and often works around them.
	private static final int EXCESSIVE_EMPTY = 100;
	// maximum amount of time to wait between reconnect attempts
	private static final long MAX_DELAY = 30000;

	private Selector selector=null;
	private QueueAttachment[] connections=null;
	private int emptySelects=0;
	private ConcurrentLinkedQueue<QueueAttachment> addedQueue=null;
	private SortedMap<Long, QueueAttachment> reconnectQueue=null;

	public MemcachedConnection(int bufSize, ConnectionFactory f,
			List<InetSocketAddress> a)
		throws IOException {
		reconnectQueue=new TreeMap<Long, QueueAttachment>();
		addedQueue=new ConcurrentLinkedQueue<QueueAttachment>();
		selector=Selector.open();
		connections=new QueueAttachment[a.size()];
		int cons=0;
		for(SocketAddress sa : a) {
			SocketChannel ch=SocketChannel.open();
			ch.configureBlocking(false);
			QueueAttachment qa=new QueueAttachment(sa, ch, bufSize,
				f.createOperationQueue());
			qa.which=cons;
			int ops=0;
			if(ch.connect(sa)) {
				getLogger().info("Connected to %s immediately", qa);
				qa.reconnectAttempt=0;
				assert ch.isConnected();
			} else {
				getLogger().info("Added %s to connect queue", qa);
				ops=SelectionKey.OP_CONNECT;
			}
			qa.sk=ch.register(selector, ops, qa);
			assert ch.isConnected()
				|| qa.sk.interestOps() == SelectionKey.OP_CONNECT
				: "Not connected, and not wanting to connect";
			connections[cons++]=qa;
		}
	}

	private boolean selectorsMakeSense() {
		for(QueueAttachment qa : connections) {
			synchronized(qa) {
				if(qa.sk.isValid()) {
					if(qa.channel.isConnected()) {
						Operation op=qa.ops.peek();
						int sops=qa.sk.interestOps();
						if(op == null) {
							assert sops == 0 : "Invalid ops: " + qa;
						} else {
							switch(op.getState()) {
							case READING:
								assert (sops & SelectionKey.OP_READ) != 0
									: "Invalid ops: " + qa;
								break;
							case WRITING:
								assert (sops & SelectionKey.OP_WRITE) != 0
									: "Invalid ops: " + qa;
								break;
							case COMPLETE:
								assert false : "Completed item in queue";
							}
						}
					} else {
						int sops=qa.sk.interestOps();
						assert sops == SelectionKey.OP_CONNECT
							: "Not connected, and not watching for connect: "
								+ sops;
					}
				}
			}
		}
		getLogger().debug("Checked the selectors.");
		return true;
	}

	@SuppressWarnings("unchecked")
	public void handleIO() throws IOException {

		// Deal with all of the stuff that's been added, but may not be marked
		// writable.
		handleInputQueue();
		getLogger().debug("Done dealing with queue.");

		long delay=0;
		if(!reconnectQueue.isEmpty()) {
			long now=System.currentTimeMillis();
			long then=reconnectQueue.firstKey();
			delay=Math.max(then-now, 1);
		}
		getLogger().debug("Selecting with delay of %sms", delay);
		assert selectorsMakeSense() : "Selectors don't make sense.";
		int selected=selector.select(delay);
		if(selected > 0) {
			Set<SelectionKey> selectedKeys=selector.selectedKeys();
			assert selected == selectedKeys.size();
			getLogger().debug("Selected %d, selected %d keys",
					selected, selectedKeys.size());
			emptySelects=0;
			for(SelectionKey sk : selectedKeys) {
				getLogger().debug(
						"Got selection key:  %s (r=%s, w=%s, c=%s, op=%s)",
						sk, sk.isReadable(), sk.isWritable(),
						sk.isConnectable(), sk.attachment());
				handleIO(sk);
			} // for each selector
			selectedKeys.clear();
		} else {
			// It's very easy in NIO to write a bug such that your selector
			// spins madly.  This will catch that and let it break.
			getLogger().debug("No selectors ready, interrupted: "
					+ Thread.interrupted());
			if(++emptySelects > EXCESSIVE_EMPTY) {
				for(SelectionKey sk : selector.keys()) {
					getLogger().info("%s has %s, interested in %s",
							sk, sk.readyOps(), sk.interestOps());
					if(sk.readyOps() != 0) {
						getLogger().info("%s has a ready op, handling IO", sk);
						handleIO(sk);
					} else {
						queueReconnect((QueueAttachment)sk.attachment());
					}
				}
				assert emptySelects < EXCESSIVE_EMPTY + 10
					: "Too many empty selects";
			}
		}
		if(!reconnectQueue.isEmpty()) {
			attemptReconnects();
		}
	}

	private void handleInputQueue() {
		if(!addedQueue.isEmpty()) {
			getLogger().debug("Handling queue");
			// If there's stuff in the added queue.  Try to process it.
			Collection<QueueAttachment> toAdd=new HashSet<QueueAttachment>();
			try {
				QueueAttachment qa=null;
				while((qa=addedQueue.remove()) != null) {
					if(qa.channel != null && qa.channel.isConnected()) {
						Operation op=qa.ops.peek();
						if(op != null
								&& op.getState() == Operation.State.WRITING) {
							getLogger().debug(
									"Handling queued write on %s", qa);
							try {
								handleOperation(op, qa.sk, qa);
							} catch(IOException e) {
								getLogger().warn("Exception handling %s",
										op, e);
								queueReconnect(qa);
							}
						}
					} else {
						toAdd.add(qa);
					}
				}
			} catch(NoSuchElementException e) {
				// out of stuff.
			}
			addedQueue.addAll(toAdd);
		}
	}

	// Handle IO for a specific selector.  Any IOException will cause a
	// reconnect
	private void handleIO(SelectionKey sk) {
		QueueAttachment qa=(QueueAttachment)sk.attachment();
		if(sk.isConnectable()) {
			getLogger().info("Connection state changed for %s", sk);
			try {
				if(qa.channel.finishConnect()) {
					assert qa.channel.isConnected() : "Not connected.";
					synchronized(qa) {
						qa.reconnectAttempt=0;
					}
					sk.interestOps(0);
					addedQueue.offer(qa);
				} else {
					assert !qa.channel.isConnected() : "connected";
				}
			} catch(IOException e) {
				getLogger().warn("Problem handling connect", e);
				queueReconnect(qa);
			}
		} else {
			Operation currentOp=qa.ops.peek();
			if(currentOp != null) {
				try {
					handleOperation(currentOp, sk, qa);
				} catch(IOException e) {
					getLogger().warn("Exception handling %s, reconnecting",
							currentOp, e);
					queueReconnect(qa);
				}
			} else {
				if(sk.isReadable()) {
					ByteBuffer b=ByteBuffer.allocate(1);
					try {
						int read=qa.channel.read(b);
						assert read == -1
							: "expected to read -1 bytes, read " + read;
					} catch(IOException e) {
						getLogger().warn("IOException reading while not"
								+ " expecting a readable channel", e);
					}
					queueReconnect(qa);
				} else {
					assert false : "No current operations, but selectors ready";
				}
			}
		}
	}

	// Make a debug string out of the given buffer's values
	static String dbgBuffer(ByteBuffer b, int size) {
		StringBuilder sb=new StringBuilder();
		byte[] bytes=b.array();
		for(int i=0; i<size; i++) {
			char ch=(char)bytes[i];
			if(Character.isWhitespace(ch) || Character.isLetterOrDigit(ch)) {
				sb.append(ch);
			} else {
				sb.append("\\x");
				sb.append(Integer.toHexString(bytes[i] & 0xff));
			}
		}
		return sb.toString();
	}

	// Handle IO for an operation.
	private void handleOperation(Operation currentOp, SelectionKey sk,
			QueueAttachment qa) throws IOException {
		getLogger().debug("Current operation: %s", currentOp);
		// First switch is for IO.
		switch(currentOp.getState()) {
			case READING:
				assert !sk.isWritable() : "While reading, came up writable";
				if(sk.isReadable()) {
					int read=qa.channel.read(qa.buf);
					if(read < 0) {
						queueReconnect(qa);
					} else {
						qa.buf.flip();
						currentOp.readFromBuffer(qa.buf);
						qa.buf.clear();
					}
				} else {
					assert false : "While reading, came up not readable.";
				}
				break;
			case WRITING:
				boolean mustReinint=false;
				if(sk.isValid() && sk.isReadable()) {
					getLogger().debug("Readable in write mode.");
					ByteBuffer b=ByteBuffer.allocate(512);
					int read=qa.channel.read(b);
					getLogger().debug("Read %d bytes in write mode", read);
					if(read > 0) {
						b.flip();
						getLogger().error(
							"Read %d bytes in write mode (%s) -- reconnecting",
							read, dbgBuffer(b, read));
						mustReinint=true;
					}
				}
				if(mustReinint) {
					queueReconnect(qa);
				} else {
					ByteBuffer b=currentOp.getBuffer();
					int wrote=qa.channel.write(b);
					getLogger().debug("Wrote %d bytes for %s",
							wrote, currentOp);
					if(b.remaining() == 0) {
						currentOp.writeComplete();
					}
				}
				break;
			case COMPLETE:
				assert false : "Current op is in complete state";
				break;
			default:
				assert false;
		}
		// Second switch is for post-IO examination and state transition
		switch(currentOp.getState()) {
			case READING:
				if(sk.isValid()) {
					sk.interestOps(SelectionKey.OP_READ);
				}
				break;
			case WRITING:
				getLogger().debug("Operation is still writing (%d remaining).",
					currentOp.getBuffer().remaining());
				if(sk.isValid()) {
					sk.interestOps(SelectionKey.OP_WRITE);
				}
				break;
			case COMPLETE:
				qa.ops.remove();
				// If there are more operations in the queue, tell
				// it we want to write
				if(sk.isValid()) {
					sk.interestOps(0);
				}
				synchronized(qa) {
					// After removing the cancelled operations, if there's
					// another operation waiting to go, wait for write
					if(hasPendingOperations(qa) && sk.isValid()) {
						sk.interestOps(SelectionKey.OP_WRITE);
						addedQueue.offer(qa);
					}
				}
				break;
			default:
				assert false;
		}
	}

	private boolean hasPendingOperations(QueueAttachment qa) {
		assert Thread.holdsLock(qa) : "Not locking qa";
		Operation nextOp=qa.ops.peek();
		while(nextOp != null && nextOp.isCancelled()) {
			getLogger().info("Removing cancelled operation: %s",
					nextOp);
			qa.ops.remove();
			nextOp=qa.ops.peek();
		}
		return nextOp != null;
	}

	private void queueReconnect(QueueAttachment qa) {
		synchronized(qa) {
			getLogger().warn("Closing, and reopening %s, attempt %d.",
					qa, qa.reconnectAttempt);
			qa.sk.cancel();
			assert !qa.sk.isValid() : "Cancelled selection key is valid";
			qa.reconnectAttempt++;
			try {
				qa.channel.socket().close();
			} catch(IOException e) {
				getLogger().warn("IOException trying to close a socket", e);
			}
			qa.channel=null;

			long delay=Math.min((100*qa.reconnectAttempt) ^ 2, MAX_DELAY);

			reconnectQueue.put(System.currentTimeMillis() + delay, qa);

			// Need to do a little queue management.
			setupResend(qa);
		}
	}

	private void attemptReconnects() throws IOException {
		long now=System.currentTimeMillis();
		for(Iterator<QueueAttachment> i=
				reconnectQueue.headMap(now).values().iterator(); i.hasNext();) {
			QueueAttachment qa=i.next();
			i.remove();
			getLogger().info("Reconnecting %s", qa);
			SocketChannel ch=SocketChannel.open();
			ch.configureBlocking(false);
			int ops=0;
			if(ch.connect(qa.socketAddress)) {
				getLogger().info("Immediately reconnected to %s", qa);
				assert ch.isConnected();
			} else {
				ops=SelectionKey.OP_CONNECT;
			}
			qa.channel=ch;
			qa.sk=ch.register(selector, ops, qa);
		}
	}

	private void setupResend(QueueAttachment qa) {
		Operation op=qa.ops.peek();
		if(op != null) {
			if(op.getState() == Operation.State.WRITING) {
				getLogger().warn("Resetting write state of op: %s", op);
				op.getBuffer().reset();
				addedQueue.offer(qa);
			} else {
				getLogger().warn(
						"Discarding partially completed operation: %s", op);
				op.cancel();
				qa.ops.remove();
			}
		}
	}

	/**
	 * Get the number of connections currently handled.
	 */
	public int getNumConnections() {
		return connections.length;
	}

	/**
	 * Get the remote address of the socket with the given ID.
	 * 
	 * @param which which id
	 * @return the rmeote address
	 */
	public SocketAddress getAddressOf(int which) {
		return connections[which].socketAddress;
	}

	/**
	 * Add an operation to the given connection.
	 * 
	 * @param which the connection offset
	 * @param o the operation
	 */
	@SuppressWarnings("unchecked")
	public void addOperation(int which, Operation o) {
		QueueAttachment qa=connections[which];
		o.initialize();
		synchronized(qa) {
			boolean wasEmpty=qa.ops.isEmpty();
			boolean added=qa.ops.add(o);
			assert added; // documented to throw an IllegalStateException
			if(wasEmpty && qa.sk.isValid() && qa.channel.isConnected()) {
				qa.sk.interestOps(SelectionKey.OP_WRITE);
			}
		}
		addedQueue.offer(qa);
		selector.wakeup();
		getLogger().debug("Added %s to %d", o, which);
	}

	/**
	 * Shut down all of the connections.
	 */
	public void shutdown() throws IOException {
		for(QueueAttachment qa : connections) {
			qa.channel.close();
			qa.sk=null;
			getLogger().debug("Shut down channel %s", qa.channel);
		}
		selector.close();
		getLogger().debug("Shut down selector %s", selector);
	}

	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();
		sb.append("{MemcachedConnection to");
		for(QueueAttachment qa : connections) {
			sb.append(" ");
			sb.append(qa.socketAddress);
		}
		sb.append("}");
		return sb.toString();
	}

	private static class QueueAttachment {
		public int which=0;
		public int reconnectAttempt=1;
		public SocketAddress socketAddress=null;
		public SocketChannel channel=null;
		public ByteBuffer buf=null;
		public BlockingQueue<Operation> ops=null;
		public SelectionKey sk=null;
		public QueueAttachment(SocketAddress sa, SocketChannel c, int bufSize,
				BlockingQueue<Operation> q) {
			super();
			assert sa != null : "No SocketAddress";
			assert c != null : "No SocketChannel";
			assert bufSize > 0 : "Invalid buffer size: " + bufSize;
			assert q != null : "No operation queue";
			socketAddress=sa;
			channel=c;
			buf=ByteBuffer.allocate(bufSize);
			ops=q;
		}

		@Override
		public String toString() {
			int sops=0;
			if(sk!= null && sk.isValid()) {
				sops=sk.interestOps();
			}
			return "{QA sa=" + socketAddress + ", #ops=" + ops.size()
				+ ", topop=" + ops.peek() + ", interested=" + sops + "}";
		}
	}
}
