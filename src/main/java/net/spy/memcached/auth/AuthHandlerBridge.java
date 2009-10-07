package net.spy.memcached.auth;

import javax.security.auth.callback.CallbackHandler;

import net.spy.memcached.ops.OperationCallback;

/**
 * Interface for bridging auth callback to an operation callback.
 */
public interface AuthHandlerBridge extends CallbackHandler, OperationCallback {
	// Just the union of the two interfaces.
}
