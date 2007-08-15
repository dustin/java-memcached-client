package net.spy.memcached.ops;

/**
 * Type of mutation to perform.
 */
public enum Mutator {
	/**
	 * Increment a value on the memcached server.
	 */
	incr,
	/**
	 * Decrement a value on the memcached server.
	 */
	decr
}