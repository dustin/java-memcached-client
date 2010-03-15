package net.spy.memcached.auth;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import net.spy.memcached.KeyUtil;
import net.spy.memcached.MemcachedConnection;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.OperationFactory;
import net.spy.memcached.compat.SpyThread;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;

public class AuthThread extends SpyThread {

	private final MemcachedConnection conn;
	private final AuthDescriptor authDescriptor;
	private final OperationFactory opFact;
	private final MemcachedNode node;

	public AuthThread(MemcachedConnection c, OperationFactory o,
			AuthDescriptor a, MemcachedNode n) {
		conn = c;
		opFact = o;
		authDescriptor = a;
		node = n;
		start();
	}

	@Override
	public void run() {
		OperationStatus priorStatus = null;
		final AtomicBoolean done = new AtomicBoolean();

		while(!done.get()) {
			final CountDownLatch latch = new CountDownLatch(1);
			final AtomicReference<OperationStatus> foundStatus =
				new AtomicReference<OperationStatus>();

			final OperationCallback cb=new OperationCallback() {
				public void receivedStatus(OperationStatus val) {
					// If the status we found was null, we're done.
					if(val.getMessage().isEmpty()) {
						done.set(true);
						node.authComplete();
						getLogger().info("Authenticated to "
								+ node.getSocketAddress());
					} else {
						foundStatus.set(val);
					}
				}

				public void complete() {
					latch.countDown();
				}
			};

			// Get the prior status to create the correct operation.
			final Operation op = buildOperation(priorStatus, cb);

			conn.insertOperation(node, op);

			try {
				latch.await();
				Thread.sleep(100);
			} catch(InterruptedException e) {
				// we can be interrupted if we were in the
				// process of auth'ing and the connection is
				// lost or dropped due to bad auth
				Thread.currentThread().interrupt();
				if (op != null) {
					op.cancel();
				}
				done.set(true); // If we were interrupted, tear down.
			}

			// Get the new status to inspect it.
			priorStatus = foundStatus.get();
			if(priorStatus != null) {
				if(!priorStatus.isSuccess()) {
					getLogger().warn("Authentication failed to "
							+ node.getSocketAddress());
				}
			}
		}
		return;
	}

	private Operation buildOperation(OperationStatus st, OperationCallback cb) {
		if(st == null) {
			return opFact.saslAuth(authDescriptor.mechs,
					node.getSocketAddress().toString(), null,
					authDescriptor.cbh, cb);
		} else {
			return opFact.saslStep(authDescriptor.mechs,
					KeyUtil.getKeyBytes(st.getMessage()),
					node.getSocketAddress().toString(), null,
					authDescriptor.cbh, cb);
		}

	}
}
