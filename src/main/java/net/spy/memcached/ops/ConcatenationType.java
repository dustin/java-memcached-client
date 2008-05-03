package net.spy.memcached.ops;

/**
 * Types of concatenation operations.
 */
public enum ConcatenationType {
	/**
	 * Concatenate supplied data to the end of the existing data.
	 */
	append,
	/**
	 * Concatenate existing data onto the end of the supplied data.
	 */
	prepend
}
