package net.spy.memcached.ops;

import java.util.Collection;

/**
 * Get operation.
 */
public interface GetOperation extends Operation {

	/**
	 * Get the keys requested in this GetOperation.
	 */
	Collection<String> getKeys();

}