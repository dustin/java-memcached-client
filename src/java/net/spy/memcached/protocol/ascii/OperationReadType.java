package net.spy.memcached.protocol.ascii;

/**
 * Data read types.
 */
enum OperationReadType {
	/**
	 * Read type indicating an operation currently wants to read lines.
	 */
	LINE,
	/**
	 * Read type indicating an operation currently wants to read raw data.
	 */
	DATA
}