package net.spy.memcached.tapmessage;

/**
 * The Opcode enum contains a list all of the different opcodes that can be passed in a tap message in the flag field.
 */
public enum TapOpcode {
	/**
	 * Defines a tap no-op message
	 */
	NOOP((byte) 0x0a),

	/**
	 * Defines a SASL list mechanism message
	 */
	SASLLIST((byte) 0x20),

	/**
	 * Defines a SASL authorization message
	 */
	SASLAUTH((byte) 0x21),

	/**
	 * Defines a request message to open a tap connection
	 */
	REQUEST((byte) 0x40),

	/**
	 * Defines a key-value mutation message to specify a key-value has changed
	 */
	MUTATION((byte) 0x41),

	/**
	 * Defines a delete message to specify a key has been deleted
	 */
	DELETE((byte) 0x42),

	/**
	 * Defines a tap flush message
	 */
	FLUSH((byte) 0x43),

	/**
	 * Defines a opaque message to send control data to the consumer
	 */
	OPAQUE((byte)0x44),

	/**
	 * Defines a vBucket set message to set the state of a vBucket in the consumer
	 */
	VBUCKETSET((byte) 0x45);

	/**
	 * The opcode value
	 */
	public byte opcode;

	/**
	 * Defines the magic value
	 * @param magic - The new magic value
	 */
	TapOpcode(byte opcode) {
		this.opcode = opcode;
	}

	public static TapOpcode getOpcodeByByte(byte b) {
		if (b == TapOpcode.DELETE.opcode) {
			return TapOpcode.DELETE;
		} else if (b == TapOpcode.FLUSH.opcode){
			return TapOpcode.DELETE;
		} else if (b == TapOpcode.MUTATION.opcode){
			return TapOpcode.MUTATION;
		} else if (b == TapOpcode.NOOP.opcode){
			return TapOpcode.NOOP;
		} else if (b == TapOpcode.OPAQUE.opcode){
			return TapOpcode.OPAQUE;
		} else if (b == TapOpcode.REQUEST.opcode){
			return TapOpcode.REQUEST;
		} else if (b == TapOpcode.SASLAUTH.opcode){
			return TapOpcode.SASLAUTH;
		} else if (b == TapOpcode.SASLLIST.opcode){
			return TapOpcode.SASLLIST;
		} else if (b == TapOpcode.VBUCKETSET.opcode){
			return TapOpcode.VBUCKETSET;
		} else {
			return null;
		}
	}
}
