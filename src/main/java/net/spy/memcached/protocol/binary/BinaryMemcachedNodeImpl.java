package net.spy.memcached.protocol.binary;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;

import net.spy.memcached.ops.CASOperation;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.StoreOperation;
import net.spy.memcached.protocol.ProxyCallback;
import net.spy.memcached.protocol.TCPMemcachedNodeImpl;

/**
 * Implementation of MemcachedNode for speakers of the binary protocol.
 */
public class BinaryMemcachedNodeImpl extends TCPMemcachedNodeImpl {

	private final int MAX_SET_OPTIMIZATION_COUNT = 65535;
	private final int MAX_SET_OPTIMIZATION_BYTES = 2 * 1024 * 1024;

	public BinaryMemcachedNodeImpl(SocketAddress sa, SocketChannel c,
			int bufSize, BlockingQueue<Operation> rq,
			BlockingQueue<Operation> wq, BlockingQueue<Operation> iq,
			Long opQueueMaxBlockTimeNs, boolean waitForAuth) {
		super(sa, c, bufSize, rq, wq, iq, opQueueMaxBlockTimeNs,
			waitForAuth);
	}

	@Override
	protected void optimize() {
		Operation firstOp = writeQ.peek();
		if(firstOp instanceof GetOperation) {
			optimizeGets();
		} else if(firstOp instanceof CASOperation) {
			optimizeSets();
		}
	}

	private void optimizeGets() {
		// make sure there are at least two get operations in a row before
		// attempting to optimize them.
		optimizedOp=writeQ.remove();
		if(writeQ.peek() instanceof GetOperation) {
			OptimizedGetImpl og=new OptimizedGetImpl(
					(GetOperation)optimizedOp);
			optimizedOp=og;

			while(writeQ.peek() instanceof GetOperation) {
				GetOperation o=(GetOperation) writeQ.remove();
				if(!o.isCancelled()) {
					og.addOperation(o);
				}
			}

			// Initialize the new mega get
			optimizedOp.initialize();
			assert optimizedOp.getState() == OperationState.WRITING;
			ProxyCallback pcb=(ProxyCallback) og.getCallback();
			getLogger().debug("Set up %s with %s keys and %s callbacks",
					this, pcb.numKeys(), pcb.numCallbacks());
		}
	}

	private void optimizeSets() {
		// make sure there are at least two get operations in a row before
		// attempting to optimize them.
		optimizedOp=writeQ.remove();
		if(writeQ.peek() instanceof CASOperation) {
			OptimizedSetImpl og=new OptimizedSetImpl(
					(CASOperation)optimizedOp);
			optimizedOp=og;

			while(writeQ.peek() instanceof StoreOperation
					&& og.size() < MAX_SET_OPTIMIZATION_COUNT
					&& og.bytes() < MAX_SET_OPTIMIZATION_BYTES) {
				CASOperation o=(CASOperation) writeQ.remove();
				if(!o.isCancelled()) {
					og.addOperation(o);
				}
			}

			// Initialize the new mega set
			optimizedOp.initialize();
			assert optimizedOp.getState() == OperationState.WRITING;
		}
	}
}
