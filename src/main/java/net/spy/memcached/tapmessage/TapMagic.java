package net.spy.memcached.tapmessage;

/**
 * The Magic enum contains a list all of the different magic that can be passed in a tap message in the flag field.
 */
public enum TapMagic {
	/**
	 * Defines a tap binary request packet
	 */
	PROTOCOL_BINARY_REQ((byte) 0x80),

	/**
	 * Defines a tap binary response packet
	 */
	PROTOCOL_BINARY_RES((byte) 0x81);

	/**
	 * The magic value
	 */
	public byte magic;

	/**
	 * Defines the magic value
	 * @param magic - The new magic value
	 */
	TapMagic(byte magic) {
		this.magic = magic;
	}

	public static TapMagic getMagicByByte(byte b) {
		if (b == PROTOCOL_BINARY_REQ.magic) {
			return TapMagic.PROTOCOL_BINARY_REQ;
		} else if (b == PROTOCOL_BINARY_RES.magic) {
			return TapMagic.PROTOCOL_BINARY_RES;
		} else {
			throw new IllegalArgumentException("Bad magic value");
		}
	}
}
