package net.spy.memcached.protocol.ascii;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;

import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.protocol.ProxyCallback;
import net.spy.memcached.protocol.TCPMemcachedNodeImpl;

/**
 * Memcached node for the ASCII protocol.
 */
public final class AsciiMemcachedNodeImpl extends TCPMemcachedNodeImpl {

	public AsciiMemcachedNodeImpl(SocketAddress sa, SocketChannel c,
			int bufSize, BlockingQueue<Operation> rq,
			BlockingQueue<Operation> wq, BlockingQueue<Operation> iq) {
		super(sa, c, bufSize, rq, wq, iq);
	}

	@Override
	protected void optimize() {
		// make sure there are at least two get operations in a row before
		// attempting to optimize them.
		if(writeQ.peek() instanceof GetOperation) {
			getOp=(GetOperationImpl)writeQ.remove();
			if(writeQ.peek() instanceof GetOperation) {
				OptimizedGetImpl og=new OptimizedGetImpl(getOp);
				getOp=og;

				while(writeQ.peek() instanceof GetOperation) {
					GetOperationImpl o=(GetOperationImpl) writeQ.remove();
					if(!o.isCancelled()) {
						og.addOperation(o);
					}
				}

				// Initialize the new mega get
				getOp.initialize();
				assert getOp.getState() == OperationState.WRITING;
				ProxyCallback pcb=(ProxyCallback) og.getCallback();
				getLogger().debug("Set up %s with %s keys and %s callbacks",
					this, pcb.numKeys(), pcb.numCallbacks());
			}
		}
	}

}
