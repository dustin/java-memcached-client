package net.spy.memcached.tapmessage;

/**
 * The Flag enum contains a list all of the different flags that can be passed
 * in a tap message in the flag field.
 */
public enum TapFlag {
	/**
	 * Tap backfill flag definition
	 */
	BACKFILL((byte) 0x01),

	/**
	 * Tap dump flag definition
	 */
	DUMP((byte) 0x02),

	/**
	 * Tap list vBuckets flag definition
	 */
	LIST_VBUCKETS((byte) 0x04),

	/**
	 * Tap take over vBuckets flag definition
	 */
	TAKEOVER_VBUCKETS((byte) 0x08),

	/**
	 * Tap support acknowledgment flag definition
	 */
	SUPPORT_ACK((byte) 0x10),

	/**
	 * Tap send keys only flag definition
	 */
	KEYS_ONLY((byte) 0x20),

	/**
	 * Tap use checkpoints
	 */
	CHECKPOINT((byte) 0x40);

	/**
	 * The flag value
	 */
	public byte flag;

	/**
	 * Defines the flag value
	 *
	 * @param flag - The new flag value
	 */
	TapFlag(byte flag) {
		this.flag = flag;
	}

	/**
	 * Checks to see if a flag is contained in a flag field. The flag field must be converted to
	 * an integer before calling this function
	 *
	 * @param f The integer value of the flag field in a tap packet
	 *
	 * @return Returns true if the flag is contained in the flag field
	 */
	boolean hasFlag(int f) {
		if ((f & (int) flag) == 1) {
			return false;
		}
		return true;
	}
}
