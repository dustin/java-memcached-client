package net.spy.memcached;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import net.spy.memcached.ops.Operation;

/**
 * Interface defining a connection to a memcached server.
 */
public interface MemcachedNode {

	/**
	 * Move all of the operations delivered via addOperation into the internal
	 * write queue.
	 */
	void copyInputQueue();

	/**
	 * Clear the queue of currently processing operations by either cancelling
	 * them or setting them up to be reapplied after a reconnect.
	 */
	void setupResend();

	/**
	 * Fill the write buffer with data from the next operations in the queue.
	 *
	 * @param optimizeGets if true, combine sequential gets into a single
	 *                     multi-key get
	 */
	void fillWriteBuffer(boolean optimizeGets);

	/**
	 * Transition the current write item into a read state.
	 */
	void transitionWriteItem();

	/**
	 * Get the operation at the top of the queue that is requiring input.
	 */
	Operation getCurrentReadOp();

	/**
	 * Remove the operation at the top of the queue that is requiring input.
	 */
	Operation removeCurrentReadOp();

	/**
	 * Get the operation at the top of the queue that has information available
	 * to write.
	 */
	Operation getCurrentWriteOp();

	/**
	 * Remove the operation at the top of the queue that has information
	 * available to write.
	 */
	Operation removeCurrentWriteOp();

	/**
	 * True if an operation is available to read.
	 */
	boolean hasReadOp();

	/**
	 * True if an operation is available to write.
	 */
	boolean hasWriteOp();

	/**
	 * Add an operation to the queue.
	 */
	void addOp(Operation op);

	/**
	 * Compute the appropriate selection operations for the channel this
	 * MemcachedNode holds to the server.
	 */
	int getSelectionOps();

	/**
	 * Get the buffer used for reading data from this node.
	 */
	ByteBuffer getRbuf();

	/**
	 * Get the buffer used for writing data to this node.
	 */
	ByteBuffer getWbuf();

	/**
	 * Get the SocketAddress of the server to which this node is connected.
	 */
	SocketAddress getSocketAddress();

	/**
	 * True if this node is <q>active.</q>  i.e. is is currently connected
	 * and expected to be able to process requests
	 */
	boolean isActive();

	/**
	 * Notify this node that it will be reconnecting.
	 */
	void reconnecting();

	/**
	 * Notify this node that it has reconnected.
	 */
	void connected();

	/**
	 * Get the current reconnect count.
	 */
	int getReconnectCount();

	/**
	 * Register a channel with this node.
	 */
	void registerChannel(SocketChannel ch, SelectionKey selectionKey);

	/**
	 * Set the SocketChannel this node uses.
	 */
	void setChannel(SocketChannel to);

	/**
	 * Get the SocketChannel for this connection.
	 */
	SocketChannel getChannel();

	/**
	 * Set the selection key for this node.
	 */
	void setSk(SelectionKey to);

	/**
	 * Get the selection key from this node.
	 */
	SelectionKey getSk();

	/**
	 * Get the number of bytes remaining to write.
	 */
	int getBytesRemainingToWrite();

	/**
	 * Write some bytes and return the number of bytes written.
	 * @return the number of bytes written
	 * @throws IOException if there's a problem writing
	 */
	int writeSome() throws IOException;

	/**
	 * Fix up the selection ops on the selection key.
	 */
	void fixupOps();

}