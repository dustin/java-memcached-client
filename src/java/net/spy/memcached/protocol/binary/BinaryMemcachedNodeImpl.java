package net.spy.memcached.protocol.binary;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;

import net.spy.memcached.ops.Operation;
import net.spy.memcached.protocol.TCPMemcachedNodeImpl;

/**
 * Implementation of MemcachedNode for speakers of the binary protocol.
 */
public class BinaryMemcachedNodeImpl extends TCPMemcachedNodeImpl {

	public BinaryMemcachedNodeImpl(SocketAddress sa, SocketChannel c,
			int bufSize, BlockingQueue<Operation> rq,
			BlockingQueue<Operation> wq, BlockingQueue<Operation> iq) {
		super(sa, c, bufSize, rq, wq, iq);
	}

	@Override
	protected void optimize() {
		// TODO:  Support a sequence of gets to quiet gets and a noop
	}

}
