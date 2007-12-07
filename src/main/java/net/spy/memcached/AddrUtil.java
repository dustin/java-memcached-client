package net.spy.memcached;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Convenience utilities for simplifying common address parsing.
 */
public class AddrUtil {

	/**
	 * Split a string in the form of "host:port host2:port" into a List of
	 * InetSocketAddress instances suitable for instantiating a MemcachedClient.
	 */
	public static List<InetSocketAddress> getAddresses(String s) {
		if(s == null) {
			throw new NullPointerException("Null host list");
		}
		if(s.trim().equals("")) {
			throw new IllegalArgumentException("No hosts in list:  ``"
					+ s + "''");
		}
		ArrayList<InetSocketAddress> addrs=
			new ArrayList<InetSocketAddress>();

		for(String hoststuff : s.split(" ")) {
			String[] parts=hoststuff.split(":");
			if(parts.length != 2) {
				throw new IllegalArgumentException("Invalid server ``"
						+ hoststuff + "'' in list:  " + s);
			}

			addrs.add(new InetSocketAddress(parts[0],
					Integer.parseInt(parts[1])));
		}
		assert !addrs.isEmpty() : "No addrs found";
		return addrs;
	}
}
