package net.spy.memcached.tapmessage;

public class MessageBuilder {
	RequestMessage message;

	public MessageBuilder() {
		this.message = new RequestMessage();
		message.setMagic(TapMagic.PROTOCOL_BINARY_REQ);
		message.setOpcode(TapOpcode.REQUEST);
	}

	public void doBackfill(long date) {
		message.setBackfill(date);
		message.setFlags(TapFlag.BACKFILL);
	}

	public void doDump() {
		message.setFlags(TapFlag.DUMP);
	}

	public void specifyVbuckets(short[] vbucketlist) {
		message.setVbucketlist(vbucketlist);
		message.setFlags(TapFlag.LIST_VBUCKETS);
	}

	public void supportAck() {
		message.setFlags(TapFlag.SUPPORT_ACK);
	}

	public void keysOnly() {
		message.setFlags(TapFlag.KEYS_ONLY);
	}

	public void takeoverVbuckets(short[] vbucketlist) {
		message.setVbucketlist(vbucketlist);
		message.setFlags(TapFlag.TAKEOVER_VBUCKETS);
	}

	public RequestMessage getMessage() {
		return message;
	}
}
