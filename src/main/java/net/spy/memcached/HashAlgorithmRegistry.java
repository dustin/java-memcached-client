package net.spy.memcached;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of known hashing algorithms for locating a server for a key. Useful
 * when configuring from files using algorithm names.
 *
 * <p>
 * Please, make sure you register your algorithm with {
 * {@link #registerHashAlgorithm(String, HashAlgorithm)} before referring to it
 * by name
 */
public final class HashAlgorithmRegistry {

	/**
	 * Internal registry storage
	 */
	private static final Map<String, HashAlgorithm> REGISTRY = new HashMap<
			String, HashAlgorithm>();
	// initializing with the default algorithms implementation
	static {
		for (DefaultHashAlgorithm alg : DefaultHashAlgorithm.values()) {
			registerHashAlgorithm(
					alg.name().replace("[_HASH]$", "").toLowerCase(), alg);
		}
	}

	/**
	 * Registers provided {@link HashAlgorithm} instance with the given name.
	 * Name is not case sensitive. Any registered algorithm with the same name
	 * will be substituted
	 *
	 * @param name
	 *            of the algorithm
	 * @param alg
	 *            algorithm instance to register
	 */
	public static synchronized void registerHashAlgorithm(
			String name, HashAlgorithm alg) {
		if (name != null) {
			REGISTRY.put(name.toLowerCase(), alg);
		}
	}

	/**
	 * Tries to find selected hash algorithm using name provided
	 *
	 * <p>
	 * Note, that lookup is being performed using name's lower-case value
	 *
	 * @param name
	 *            the algorithm name to be used for lookup
	 * @return a {@link HashAlgorithm} instance or <code>null</code> if there's
	 *         no algorithm with the specified name
	 */
	public static synchronized HashAlgorithm lookupHashAlgorithm(String name) {
		return name == null ? null : REGISTRY.get(name.toLowerCase());
	}
}
