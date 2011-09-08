package net.spy.memcached.protocol.binary;

import java.util.UUID;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.TapOperation;
import net.spy.memcached.tapmessage.RequestMessage;
import net.spy.memcached.tapmessage.TapFlag;
import net.spy.memcached.tapmessage.TapMagic;
import net.spy.memcached.tapmessage.TapOpcode;

public class TapDumpOperationImpl extends TapOperationImpl implements TapOperation {
	private final String id;

	TapDumpOperationImpl(String id, OperationCallback cb) {
		super(cb);
		this.id = id;
	}

	@Override
	public void initialize() {
		RequestMessage message = new RequestMessage();
		message.setMagic(TapMagic.PROTOCOL_BINARY_REQ);
		message.setOpcode(TapOpcode.REQUEST);
		message.setFlags(TapFlag.DUMP);
		message.setFlags(TapFlag.SUPPORT_ACK);
		if (id != null) {
			message.setName(id);
		} else {
			message.setName(UUID.randomUUID().toString());
		}

		setBuffer(message.getBytes());
	}

	@Override
	public void streamClosed(OperationState state) {
		transitionState(state);
	}

	@Override
	public String toString() {
		return "Cmd: tap dump Flags: dump,ack";
	}
}
