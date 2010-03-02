package net.spy.memcached.util;

import java.util.HashMap;
import java.util.Map;

import net.spy.memcached.MemcachedNode;

/**
 * A Default implementation of the configuration required for the
 * KetamaNodeLocator algorithm to run.
 */
public class DefaultKetamaNodeLocatorConfiguration implements
		KetamaNodeLocatorConfiguration {

	final int NUM_REPS=160;

	// Internal lookup map to try to carry forward the optimisation that was
	// previously in KetamaNodeLocator
	protected Map<MemcachedNode, String> socketAddresses=
		new HashMap<MemcachedNode, String>();

	/**
	 * Returns the socket address of a given MemcachedNode.
	 *
	 * @param node The node which we're interested in
	 * @return String the socket address of that node.
	 */
	protected String getSocketAddressForNode(MemcachedNode node) {
		// Using the internal map retrieve the socket addresses
		// for given nodes.
		// I'm aware that this code is inherently thread-unsafe as
		// I'm using a HashMap implementation of the map, but the worst
		// case ( I believe) is we're slightly in-efficient when
		// a node has never been seen before concurrently on two different
		// threads, so it the socketaddress will be requested multiple times!
		// all other cases should be as fast as possible.
		String result=socketAddresses.get(node);
		if(result == null) {
			result=String.valueOf(node.getSocketAddress());
			if (result.startsWith("/")) {
				result = result.substring(1);
			}
			socketAddresses.put(node, result);
		}
		return result;
	}

	/**
	 * Returns the number of discrete hashes that should be defined for each
	 * node in the continuum.
	 *
	 * @return NUM_REPS repetitions.
	 */
	public int getNodeRepetitions() {
		return NUM_REPS;
	}

	/**
	 * Returns a uniquely identifying key, suitable for hashing by the
	 * KetamaNodeLocator algorithm.
	 *
	 * <p>
	 * This default implementation uses the socket-address of the MemcachedNode
	 * and concatenates it with a hyphen directly against the repetition number
	 * for example a key for a particular server's first repetition may look
	 * like:
	 * <p>
	 *
	 * <p>
	 * <code>myhost/10.0.2.1-0</code>
	 * </p>
	 *
	 * <p>for the second repetition</p>
	 *
	 * <p><code>myhost/10.0.2.1-1</code></p>
	 *
	 * <p>
	 * for a server where reverse lookups are failing the returned keys
	 * may look like
	 * </p>
	 *
	 * <p>
	 * <code>/10.0.2.1-0</code> and <code>/10.0.2.1-1</code>
	 * </p>
	 *
	 * @param node The MemcachedNode to use to form the unique identifier
	 * @param repetition The repetition number for the particular node in
	 *            question (0 is the first repetition)
	 * @return The key that represents the specific repetition of the
	 *         node
	 */
	public String getKeyForNode(MemcachedNode node, int repetition) {
		return getSocketAddressForNode(node) + "-" + repetition;
	}
}
