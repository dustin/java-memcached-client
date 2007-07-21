package net.spy.memcached;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import net.spy.SpyObject;

/**
 * This is an implementation of the Ketama consistent hash strategy from
 * last.fm.  This implementation may not be compatible with libketama as
 * hashing is considered separate from node location.
 *
 * Note that this implementation does not currently supported weighted nodes.
 *
 * @see http://www.last.fm/user/RJ/journal/2007/04/10/392555/
 */
public final class KetamaNodeLocator extends SpyObject implements NodeLocator {

	static final int NUM_REPS = 100;

	final SortedMap<Long, MemcachedNode> ketamaNodes=
		new TreeMap<Long, MemcachedNode>();
	final Collection<MemcachedNode> allNodes;

	final HashAlgorithm hashAlg;

	public KetamaNodeLocator(List<MemcachedNode> nodes, HashAlgorithm alg) {
		super();
		allNodes = nodes;
		hashAlg = alg;

		for(MemcachedNode node : nodes) {
			// XXX:  Replace getSocketAddress() with something more precise
			String sockStr=String.valueOf(node.getSocketAddress());
			for(int i=0; i<NUM_REPS; i++) {
				long hash = hashAlg.hash(sockStr + "-" + i);
				ketamaNodes.put(hash, node);
			}
		}
	}

	public Collection<MemcachedNode> getAll() {
		return allNodes;
	}

	public MemcachedNode getPrimary(final String k) {
		MemcachedNode rv=getNodeForKey(hashAlg.hash(k));
		assert rv != null : "Found no node for key " + k;
		return rv;
	}

	MemcachedNode getNodeForKey(long hash) {
		final MemcachedNode rv;
		if(!ketamaNodes.containsKey(hash)) {
			// Java 1.6 adds a ceilingKey method, but I'm still stuck in 1.5
			// in a lot of places, so I'm doing this myself.
			SortedMap<Long, MemcachedNode> tailMap=ketamaNodes.tailMap(hash);
			if(tailMap.isEmpty()) {
				hash=ketamaNodes.firstKey();
			} else {
				hash=tailMap.firstKey();
			}
		}
		rv=ketamaNodes.get(hash);
		return rv;
	}

	public Iterator<MemcachedNode> getSequence(String k) {
		return new KetamaIterator(k, allNodes.size());
	}


	class KetamaIterator implements Iterator<MemcachedNode> {

		final String key;
		long hashVal;
		int remainingTries;
		int numTries=0;

		public KetamaIterator(final String k, final int t) {
			super();
			hashVal=hashAlg.hash(k);
			remainingTries=t;
			key=k;
		}

		private void nextHash() {
			// this.calculateHash(Integer.toString(tries)+key).hashCode();
			long tmpKey=hashAlg.hash(numTries + key);
			// This echos the implementation of Long.hashCode()
			hashVal += (int)(tmpKey ^ (tmpKey >>> 32));
			remainingTries--;
		}

		public boolean hasNext() {
			return remainingTries > 0;
		}

		public MemcachedNode next() {
			try {
				return getNodeForKey(hashVal);
			} finally {
				nextHash();
			}
		}

		public void remove() {
			throw new UnsupportedOperationException("remove not supported");
		}

	}
}
