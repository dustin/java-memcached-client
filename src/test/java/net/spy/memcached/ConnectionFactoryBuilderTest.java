package net.spy.memcached;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import junit.framework.TestCase;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.protocol.ascii.AsciiOperationFactory;
import net.spy.memcached.protocol.binary.BinaryOperationFactory;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.WhalinTranscoder;

/**
 * Test the connection factory builder.
 */
public class ConnectionFactoryBuilderTest extends TestCase {

	private ConnectionFactoryBuilder b;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		b = new ConnectionFactoryBuilder();
	}

	public void testDefaults() {
		ConnectionFactory f = b.build();
		assertEquals(DefaultConnectionFactory.DEFAULT_OPERATION_TIMEOUT,
				f.getOperationTimeout());
		assertEquals(DefaultConnectionFactory.DEFAULT_READ_BUFFER_SIZE,
				f.getReadBufSize());
		assertSame(DefaultConnectionFactory.DEFAULT_HASH, f.getHashAlg());
		assertTrue(f.getDefaultTranscoder() instanceof SerializingTranscoder);
		assertSame(DefaultConnectionFactory.DEFAULT_FAILURE_MODE,
				f.getFailureMode());
		assertEquals(0, f.getInitialObservers().size());
		assertTrue(f.getOperationFactory() instanceof AsciiOperationFactory);

		BlockingQueue<Operation> opQueue = f.createOperationQueue();
		assertTrue(opQueue instanceof ArrayBlockingQueue<?>);
		assertEquals(DefaultConnectionFactory.DEFAULT_OP_QUEUE_LEN,
				opQueue.remainingCapacity());

		BlockingQueue<Operation> readOpQueue = f.createReadOperationQueue();
		assertTrue(readOpQueue instanceof LinkedBlockingQueue<?>);

		BlockingQueue<Operation> writeOpQueue = f.createWriteOperationQueue();
		assertTrue(writeOpQueue instanceof LinkedBlockingQueue<?>);

		assertTrue(f.isDaemon());
		assertTrue(f.shouldOptimize());
	}

	public void testModifications() {
		ConnectionObserver testObserver = new ConnectionObserver() {
			public void connectionLost(SocketAddress sa) {
				// none
			}
			public void connectionEstablished(SocketAddress sa, int reconnectCount) {
				// none
			}
		};
		BlockingQueue<Operation> oQueue = new LinkedBlockingQueue<Operation>();
		BlockingQueue<Operation> rQueue = new LinkedBlockingQueue<Operation>();
		BlockingQueue<Operation> wQueue = new LinkedBlockingQueue<Operation>();

		ConnectionFactory f = b.setDaemon(false)
			.setShouldOptimize(false)
			.setFailureMode(FailureMode.Redistribute)
			.setHashAlg(HashAlgorithm.KETAMA_HASH)
			.setInitialObservers(Collections.singleton(testObserver))
			.setOpFact(new BinaryOperationFactory())
			.setOpTimeout(4225)
			.setOpQueue(oQueue)
			.setReadOpQueue(rQueue)
			.setWriteOpQueue(wQueue)
			.setReadBufferSize(19)
			.setTranscoder(new WhalinTranscoder())
			.build();

		assertEquals(4225, f.getOperationTimeout());
		assertEquals(19, f.getReadBufSize());
		assertSame(HashAlgorithm.KETAMA_HASH, f.getHashAlg());
		assertTrue(f.getDefaultTranscoder() instanceof WhalinTranscoder);
		assertSame(FailureMode.Redistribute, f.getFailureMode());
		assertEquals(1, f.getInitialObservers().size());
		assertSame(testObserver, f.getInitialObservers().iterator().next());
		assertTrue(f.getOperationFactory() instanceof BinaryOperationFactory);
		assertSame(oQueue, f.createOperationQueue());
		assertSame(rQueue, f.createReadOperationQueue());
		assertSame(wQueue, f.createWriteOperationQueue());
		assertFalse(f.isDaemon());
		assertFalse(f.shouldOptimize());
	}

}
