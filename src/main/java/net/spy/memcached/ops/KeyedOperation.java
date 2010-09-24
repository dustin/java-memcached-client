package net.spy.memcached.ops;

import java.util.Collection;

/**
 * Operations that contain keys.
 */
public interface KeyedOperation extends Operation, VBucketAware {

	/**
	 * Get the keys requested in this GetOperation.
	 */
	Collection<String> getKeys();

}
