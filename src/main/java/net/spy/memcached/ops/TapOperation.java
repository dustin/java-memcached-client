package net.spy.memcached.ops;

import net.spy.memcached.tapmessage.TapOpcode;
import net.spy.memcached.tapmessage.ResponseMessage;


/**
 * Tap operation.
 */
public interface TapOperation extends KeyedOperation {

	/**
	 * Operation callback for the tap dump request.
	 */
	interface Callback extends OperationCallback {
		/**
		 * Callback for each result from a get.
		 *
		 * @param message the response message sent from the server
		 */
		public void gotData(ResponseMessage message);

		public void gotAck(TapOpcode opcode, int opaque);
	}

	public void streamClosed(OperationState state);
}
