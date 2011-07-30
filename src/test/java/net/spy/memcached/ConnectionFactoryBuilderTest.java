package net.spy.memcached;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import net.spy.memcached.ConnectionFactoryBuilder.Locator;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;
import net.spy.memcached.compat.BaseMockCase;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationQueueFactory;
import net.spy.memcached.protocol.ascii.AsciiMemcachedNodeImpl;
import net.spy.memcached.protocol.ascii.AsciiOperationFactory;
import net.spy.memcached.protocol.binary.BinaryMemcachedNodeImpl;
import net.spy.memcached.protocol.binary.BinaryOperationFactory;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.WhalinTranscoder;

/**
 * Test the connection factory builder.
 */
public class ConnectionFactoryBuilderTest extends BaseMockCase {

	private ConnectionFactoryBuilder b;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		b = new ConnectionFactoryBuilder();
	}

	public void testDefaults() throws Exception {
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

		MemcachedNode n = (MemcachedNode)mock(MemcachedNode.class).proxy();
		assertTrue(f.createLocator(Collections.singletonList(n))
				instanceof ArrayModNodeLocator);

		SocketChannel sc = SocketChannel.open();
		try {
			assertTrue(f.createMemcachedNode(
					InetSocketAddress.createUnresolved("localhost", 11211),
					sc, 1)
					instanceof AsciiMemcachedNodeImpl);
		} finally {
			sc.close();
		}

		assertFalse(f.isDaemon());
		assertTrue(f.shouldOptimize());
		assertFalse(f.useNagleAlgorithm());
		assertEquals(f.getOpQueueMaxBlockTime(),
				DefaultConnectionFactory.DEFAULT_OP_QUEUE_MAX_BLOCK_TIME);
	}

	public void testModifications() throws Exception {
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

		OperationQueueFactory opQueueFactory = new DirectFactory(oQueue);
		OperationQueueFactory rQueueFactory = new DirectFactory(rQueue);
		OperationQueueFactory wQueueFactory = new DirectFactory(wQueue);
		AuthDescriptor anAuthDescriptor = new AuthDescriptor(new String[]{"PLAIN"},
			new PlainCallbackHandler("username", "password"));

		ConnectionFactory f = b.setDaemon(true)
			.setShouldOptimize(false)
			.setFailureMode(FailureMode.Redistribute)
			.setHashAlg(DefaultHashAlgorithm.KETAMA_HASH)
			.setInitialObservers(Collections.singleton(testObserver))
			.setOpFact(new BinaryOperationFactory())
			.setOpTimeout(4225)
			.setOpQueueFactory(opQueueFactory)
			.setReadOpQueueFactory(rQueueFactory)
			.setWriteOpQueueFactory(wQueueFactory)
			.setReadBufferSize(19)
			.setTranscoder(new WhalinTranscoder())
			.setUseNagleAlgorithm(true)
			.setLocatorType(Locator.CONSISTENT)
			.setOpQueueMaxBlockTime(19)
			.setAuthDescriptor(anAuthDescriptor)
			.build();

		assertEquals(4225, f.getOperationTimeout());
		assertEquals(19, f.getReadBufSize());
		assertSame(DefaultHashAlgorithm.KETAMA_HASH, f.getHashAlg());
		assertTrue(f.getDefaultTranscoder() instanceof WhalinTranscoder);
		assertSame(FailureMode.Redistribute, f.getFailureMode());
		assertEquals(1, f.getInitialObservers().size());
		assertSame(testObserver, f.getInitialObservers().iterator().next());
		assertTrue(f.getOperationFactory() instanceof BinaryOperationFactory);
		assertSame(oQueue, f.createOperationQueue());
		assertSame(rQueue, f.createReadOperationQueue());
		assertSame(wQueue, f.createWriteOperationQueue());
		assertTrue(f.isDaemon());
		assertFalse(f.shouldOptimize());
		assertTrue(f.useNagleAlgorithm());
		assertEquals(f.getOpQueueMaxBlockTime(), 19);
		assertSame(anAuthDescriptor, f.getAuthDescriptor());

		MemcachedNode n = new MockMemcachedNode(
			InetSocketAddress.createUnresolved("localhost", 11211));
		assertTrue(f.createLocator(Collections.singletonList(n))
				instanceof KetamaNodeLocator);

		SocketChannel sc = SocketChannel.open();
		try {
			assertTrue(f.createMemcachedNode(
					InetSocketAddress.createUnresolved("localhost", 11211),
					sc, 1)
					instanceof BinaryMemcachedNodeImpl);
		} finally {
			sc.close();
		}
	}

	public void testProtocolSetterBinary() {
		assertTrue(
			b.setProtocol(Protocol.BINARY).build().getOperationFactory()
			instanceof BinaryOperationFactory);
	}

	public void testProtocolSetterText() {
		assertTrue(
			b.setProtocol(Protocol.TEXT).build().getOperationFactory()
			instanceof AsciiOperationFactory);

	}

	static class DirectFactory implements OperationQueueFactory {
		private final BlockingQueue<Operation> queue;

		public DirectFactory(BlockingQueue<Operation> q) {
			super();
			queue = q;
		}

		public BlockingQueue<Operation> create() {
			return queue;
		}

	}
}
