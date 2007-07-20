package net.spy.memcached.ops;


/**
 * incr and decr operations.
 */
public interface MutatatorOperation extends Operation {

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

}