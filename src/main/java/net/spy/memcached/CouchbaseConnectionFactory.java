package net.spy.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import net.spy.memcached.couch.AsyncConnectionManager;
import net.spy.memcached.protocol.couch.HttpOperation;

public class CouchbaseConnectionFactory extends MembaseConnectionFactory {

	public CouchbaseConnectionFactory(List<URI> baseList, String bucketName,
			String usr, String pwd) throws IOException {
		super(baseList, bucketName, usr, pwd);
	}

	public CouchbaseNode createCouchDBNode(InetSocketAddress addr,
			AsyncConnectionManager connMgr) {

		return new CouchbaseNode(addr, connMgr,
				new LinkedBlockingQueue<HttpOperation>(opQueueLen),
				getOpQueueMaxBlockTime(), getOperationTimeout());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.spy.memcached.ConnectionFactory#createMemcachedConnection(java.util
	 * .List)
	 */
	public MemcachedConnection createMemcachedConnection(
			List<InetSocketAddress> addrs) throws IOException {
		return new MemcachedConnection(getReadBufSize(), this, addrs,
				getInitialObservers(), getFailureMode(), getOperationFactory());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.spy.memcached.ConnectionFactory#createCouchDBConnection(java.util
	 * .List)
	 */
	public CouchbaseConnection createCouchDBConnection(
			List<InetSocketAddress> addrs) throws IOException {
		return new CouchbaseConnection(this, addrs, getInitialObservers());
	}

}
