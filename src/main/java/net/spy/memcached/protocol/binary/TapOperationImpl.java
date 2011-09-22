package net.spy.memcached.protocol.binary;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.TapOperation;
import net.spy.memcached.tapmessage.BaseMessage;
import net.spy.memcached.tapmessage.TapFlag;
import net.spy.memcached.tapmessage.TapOpcode;
import net.spy.memcached.tapmessage.ResponseMessage;

public abstract class TapOperationImpl extends OperationImpl implements TapOperation {
	private static final byte TAP_FLAG_ACK = 0x1;

	private int bytesProcessed;
	private int bodylen;
	private byte[] header;
	private byte[] message;

	static final byte CMD=0;

	protected TapOperationImpl(OperationCallback cb) {
		super(CMD, generateOpaque(), cb);
		this.header = new byte[BaseMessage.HEADER_LENGTH];
		this.message = null;
	}

	public abstract void initialize();

	@Override
	public void readFromBuffer(ByteBuffer data) throws IOException {
		while (data.remaining() > 0) {
			if (bytesProcessed < BaseMessage.HEADER_LENGTH) {
				header[bytesProcessed] = data.get();
				bytesProcessed++;
			} else {
				if (message == null) {
					bodylen = decodeInt(header, 8);
					message = new byte[BaseMessage.HEADER_LENGTH + bodylen];
					System.arraycopy(header, 0, message, 0, BaseMessage.HEADER_LENGTH);
				}

				if (bytesProcessed < message.length) {
					message[bytesProcessed] = data.get();
					bytesProcessed++;
				}
				if (bytesProcessed >= message.length) {
					ResponseMessage response = new ResponseMessage(message);

					for (TapFlag flag : response.getFlags()) {
						if (flag.flag == TAP_FLAG_ACK) {
							((Callback)getCallback()).gotAck(response.getOpcode(), response.getOpaque());
						}
					}
					if (response.getOpcode() != TapOpcode.OPAQUE && response.getOpcode() != TapOpcode.NOOP) {
						((Callback)getCallback()).gotData(response);
					}
					message = null;
					bytesProcessed = 0;
				}
			}
		}
	}
}
