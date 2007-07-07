package net.spy.memcached;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * NodeLocator implementation for dealing with simple array lookups using a
 * modulus of the hash code and node list length.
 */
public final class ArrayModNodeLocator implements NodeLocator {

	final MemcachedNodeImpl[] nodes;

	private final HashAlgorithm hashAlg;

	/**
	 * Construct an ArraymodNodeLocator over the given array of nodes and
	 * using the given hash algorithm.
	 *
	 * @param n the array of nodes
	 * @param alg the hash algorithm
	 */
	public ArrayModNodeLocator(MemcachedNodeImpl[] n, HashAlgorithm alg) {
		super();
		nodes=n;
		hashAlg=alg;
	}

	public Collection<MemcachedNodeImpl> getAll() {
		return Arrays.asList(nodes);
	}

	public MemcachedNode getPrimary(String k) {
		return nodes[getServerForKey(k)];
	}

	public Iterator<MemcachedNodeImpl> getSequence(String k) {
		return new NodeIterator(getServerForKey(k));
	}

	private int getServerForKey(String key) {
		int rv=(int)(hashAlg.hash(key) % nodes.length);
		assert rv >= 0 : "Returned negative key for key " + key;
		assert rv < nodes.length
			: "Invalid server number " + rv + " for key " + key;
		return rv;
	}


	class NodeIterator implements Iterator<MemcachedNodeImpl> {

		private final int start;
		private int next=0;

		public NodeIterator(int keyStart) {
			start=keyStart;
			computeNext();
		}

		public boolean hasNext() {
			return next >= 0;
		}

		private void computeNext() {
			if(++next >= nodes.length) {
				next=0;
			}
			if(next == start) {
				next=-1;
			}
		}

		public MemcachedNodeImpl next() {
			return nodes[next];
		}

		public void remove() {
			throw new UnsupportedOperationException("Can't remove a node");
		}

	}
}
