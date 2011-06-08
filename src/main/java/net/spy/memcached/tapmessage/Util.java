package net.spy.memcached.tapmessage;

/**
 * The Util class provides utility functions for converting portions of byte
 * arrays to values and putting values into byte arrays.
 */
public class Util {

	private Util() {

	}

	/**
	 * Converts a field in a byte array into a value.
	 * @param buffer The byte array that contains the value
	 * @param offset The offset of where the value begins in the byte array
	 * @param length The length of the field to be converted
	 * @return A long that represent the value of the field
	 */
	public static long fieldToValue(byte[] buffer, int offset, int length) {
		long total = 0;
		long val = 0;
		for (int i = 0; i < length; i++) {
			val = buffer[offset + i];
			if (val < 0)
				val = val + 256;
			total += (long)Math.pow(256.0, (double) (length - 1 - i)) * val;
		}
		return total;
	}

	/**
	 * Puts a value into a specific location in a byte buffer.
	 * @param buffer The buffer that the value will be written to.
	 * @param offset The offset for where the value begins in the buffer.
	 * @param length The length of the field in the array
	 * @param l The value to be encoded into the byte array
	 */
	public static void valueToFieldOffest(byte[] buffer, int offset, int length, long l) {
		long divisor;
		for (int i = 0; i < length; i++) {
			divisor = (long)Math.pow(256.0, (double) (length - 1 - i));
			buffer[offset + i] = (byte) (l / divisor);
			l = l % divisor;
		}
	}
}
