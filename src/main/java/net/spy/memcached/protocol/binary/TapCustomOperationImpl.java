package net.spy.memcached.protocol.binary;

import java.util.Collection;
import java.util.UUID;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.TapOperation;
import net.spy.memcached.tapmessage.RequestMessage;

public class TapCustomOperationImpl extends TapOperationImpl implements TapOperation {
	private final String id;
	private final RequestMessage message;

	TapCustomOperationImpl(String id, RequestMessage message, OperationCallback cb) {
		super(cb);
		this.id = id;
		this.message = message;
	}

	@Override
	public void initialize() {
		if (id != null) {
			message.setName(id);
		} else {
			message.setName(UUID.randomUUID().toString());
		}
		setBuffer(message.getBytes());
	}

	/**
	 * Since the tap custom doesn't specify any specific keys to get
	 * this function always returns null;
	 */
	@Override
	public Collection<String> getKeys() {
		return null;
	}

	@Override
	public void streamClosed(OperationState state) {
		transitionState(state);
	}
}
