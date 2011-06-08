package net.spy.memcached.protocol.binary;

import java.util.Collection;
import java.util.UUID;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.TapOperation;
import net.spy.memcached.tapmessage.TapFlag;
import net.spy.memcached.tapmessage.TapMagic;
import net.spy.memcached.tapmessage.TapOpcode;
import net.spy.memcached.tapmessage.RequestMessage;

public class TapBackfillOperationImpl extends TapOperationImpl implements TapOperation {
	private final String id;
	private final long date;

	TapBackfillOperationImpl(String id, long date, OperationCallback cb) {
		super(cb);
		this.id = id;
		this.date = date;
	}

	@Override
	public void initialize() {
		RequestMessage message = new RequestMessage();
		message.setMagic(TapMagic.PROTOCOL_BINARY_REQ);
		message.setOpcode(TapOpcode.REQUEST);
		message.setFlags(TapFlag.BACKFILL);
		message.setFlags(TapFlag.SUPPORT_ACK);
		if (id != null) {
			message.setName(id);
		} else {
			message.setName(UUID.randomUUID().toString());
		}

		message.setBackfill(date);
		setBuffer(message.getBytes());
	}

	/**
	 * Since the tap backfill doesn't specify any specific keys to get
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
