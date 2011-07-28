package net.spy.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.nio.protocol.AsyncNHttpClientHandler;
import org.apache.http.nio.util.DirectByteBufferAllocator;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;

import net.spy.memcached.CouchbaseNode.EventLogger;
import net.spy.memcached.CouchbaseNode.MyHttpRequestExecutionHandler;
import net.spy.memcached.compat.SpyThread;
import net.spy.memcached.couch.AsyncConnectionManager;
import net.spy.memcached.protocol.couchdb.HttpOperation;
import net.spy.memcached.vbucket.Reconfigurable;
import net.spy.memcached.vbucket.config.Bucket;

public final class CouchbaseConnection extends SpyThread implements
		Reconfigurable {
	private static final int NUM_CONNS = 1;

	private volatile boolean shutDown;
	protected volatile boolean reconfiguring = false;
	protected volatile boolean running = true;

	private final CouchbaseConnectionFactory connFactory;
	private final ConcurrentLinkedQueue<CouchbaseNode> nodesToShutdown;
	private final Collection<ConnectionObserver> connObservers =
		new ConcurrentLinkedQueue<ConnectionObserver>();
	private List<CouchbaseNode> nodes;
	private int nextNode;

	public CouchbaseConnection(CouchbaseConnectionFactory cf,
			List<InetSocketAddress> addrs, Collection<ConnectionObserver> obs)
			throws IOException {
		shutDown = false;
		connFactory = cf;
		nodesToShutdown = new ConcurrentLinkedQueue<CouchbaseNode>();
		connObservers.addAll(obs);
		nodes = createConnections(addrs);
		nextNode = 0;
		start();
	}

	private List<CouchbaseNode> createConnections(List<InetSocketAddress> addrs)
			throws IOException {
		List<CouchbaseNode> nodeList = new LinkedList<CouchbaseNode>();

		for (InetSocketAddress a : addrs) {
			HttpParams params = new SyncBasicHttpParams();
			params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
					.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000)
					.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
					.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
					.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
					.setParameter(CoreProtocolPNames.USER_AGENT, "Spymemcached Client/1.1");

			HttpProcessor httpproc = new ImmutableHttpProcessor(
					new HttpRequestInterceptor[] { new RequestContent(),
							new RequestTargetHost(), new RequestConnControl(),
							new RequestUserAgent(), new RequestExpectContinue() });

			AsyncNHttpClientHandler protocolHandler = new AsyncNHttpClientHandler(
					httpproc, new MyHttpRequestExecutionHandler(),
					new DefaultConnectionReuseStrategy(),
					new DirectByteBufferAllocator(), params);
			protocolHandler.setEventListener(new EventLogger());

			AsyncConnectionManager connMgr = new AsyncConnectionManager(
					new HttpHost(a.getHostName(), a.getPort()), NUM_CONNS,
					protocolHandler, params);
			getLogger().info("Added %s to connect queue", a);

			CouchbaseNode node = connFactory.createCouchDBNode(a, connMgr);
			node.init();
			nodeList.add(node);
		}

		return nodeList;
	}

	public void addOp(final HttpOperation op) {
		nodes.get(getNextNode()).addOp(op);
	}

	public void handleIO() {
		for (CouchbaseNode node : nodes) {
			node.doWrites();
		}

		for (CouchbaseNode qa : nodesToShutdown) {
			nodesToShutdown.remove(qa);
			Collection<HttpOperation> notCompletedOperations = qa
					.destroyWriteQueue();
			try {
				qa.shutdown();
			} catch (IOException e) {
				getLogger().error(
						"Error shutting down connection to "
								+ qa.getSocketAddress());
			}
			redistributeOperations(notCompletedOperations);
		}
	}

	private void redistributeOperations(Collection<HttpOperation> ops) {
		int added = 0;
		for(HttpOperation op : ops) {
			addOp(op);
			added++;
		}
		assert added > 0
			: "Didn't add any new operations when redistributing";
	}

	private int getNextNode() {
		return nextNode = (++nextNode % nodes.size());
	}

	protected void checkState() {
		if (shutDown) {
			throw new IllegalStateException("Shutting down");
		}
		assert isAlive() : "IO Thread is not running.";
	}

	public boolean shutdown() throws IOException {
		if (shutDown) {
			getLogger().info("Suppressing duplicate attempt to shut down");
			return false;
		}
		shutDown = true;
		running = false;
		for (CouchbaseNode n : nodes) {
			if (n != null) {
				n.shutdown();
				if (n.hasWriteOps()) {
					getLogger().warn(
							"Shutting down with ops waiting to be written");
				}
			}
		}
		return true;
	}

	@Override
	public void reconfigure(Bucket bucket) {
		reconfiguring = true;
		try {
			// get a new collection of addresses from the received config
			List<String> servers = bucket.getConfig().getServers();
			HashSet<SocketAddress> newServerAddresses = new HashSet<SocketAddress>();
			ArrayList<InetSocketAddress> newServers = new ArrayList<InetSocketAddress>();
			for (String server : servers) {
				int finalColon = server.lastIndexOf(':');
				if (finalColon < 1) {
					throw new IllegalArgumentException("Invalid server ``"
							+ server + "'' in vbucket's server list");
				}
				String hostPart = server.substring(0, finalColon);
				// String portNum = server.substring(finalColon + 1);

				InetSocketAddress address = new InetSocketAddress(hostPart,
						Integer.parseInt("5984"));
				// add parsed address to our collections
				newServerAddresses.add(address);
				newServers.add(address);

			}

			// split current nodes to "odd nodes" and "stay nodes"
			ArrayList<CouchbaseNode> oddNodes = new ArrayList<CouchbaseNode>();
			ArrayList<CouchbaseNode> stayNodes = new ArrayList<CouchbaseNode>();
			ArrayList<InetSocketAddress> stayServers = new ArrayList<InetSocketAddress>();
			for (CouchbaseNode current : nodes) {
				if (newServerAddresses.contains(current.getSocketAddress())) {
					stayNodes.add(current);
					stayServers.add((InetSocketAddress) current
							.getSocketAddress());
				} else {
					oddNodes.add(current);
				}
			}

			// prepare a collection of addresses for new nodes
			newServers.removeAll(stayServers);

			// create a collection of new nodes
			List<CouchbaseNode> newNodes = createConnections(newServers);

			// merge stay nodes with new nodes
			List<CouchbaseNode> mergedNodes = new ArrayList<CouchbaseNode>();
			mergedNodes.addAll(stayNodes);
			mergedNodes.addAll(newNodes);

			// call update locator with new nodes list and vbucket config
			nodes = mergedNodes;

			// schedule shutdown for the oddNodes
			nodesToShutdown.addAll(oddNodes);
		} catch (IOException e) {
			getLogger().error("Connection reconfiguration failed", e);
		} finally {
			reconfiguring = false;
		}
	}

	@Override
	public void run() {
		while (running) {
			if (!reconfiguring) {
				try {
					handleIO();
				} catch (Exception e) {
					logRunException(e);
				}
			}
		}
		getLogger().info("Shut down memcached client");
	}

	private void logRunException(Exception e) {
		if (shutDown) {
			// There are a couple types of errors that occur during the
			// shutdown sequence that are considered OK. Log at debug.
			getLogger().debug("Exception occurred during shutdown", e);
		} else {
			getLogger().warn("Problem handling memcached IO", e);
		}
	}
}
