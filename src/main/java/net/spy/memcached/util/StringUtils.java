package net.spy.memcached.util;

import java.util.Collection;

/**
 * Some String utilities.
 */
public final class StringUtils {

	private StringUtils() {
		// Empty
	}

	public static String join(Collection<String> keys, String delimiter) {
		StringBuilder sb = new StringBuilder();
		for (String key : keys) {
			sb.append(key);
			sb.append(delimiter);
		}
		return sb.toString();
	}
}
