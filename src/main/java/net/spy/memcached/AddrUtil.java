package net.spy.memcached;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Convenience utilities for simplifying common address parsing.
 */
public class AddrUtil {

	/**
	 * Split a string containing whitespace or comma separated host or
	 * IP addresses and port numbers of the form "host:port host2:port"
	 * or "host:port, host2:port" into a List of InetSocketAddress
	 * instances suitable for instantiating a MemcachedClient.
	 *
	 * Note that colon-delimited IPv6 is also supported.
	 * For example:  ::1:11211
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

		for(String hoststuff : s.split("(?:\\s|,)+")) {
			if(hoststuff.equals("")) {
				continue;
			}

			int finalColon=hoststuff.lastIndexOf(':');
			if(finalColon < 1) {
				throw new IllegalArgumentException("Invalid server ``"
					+ hoststuff + "'' in list:  " + s);

			}
			String hostPart=hoststuff.substring(0, finalColon);
			String portNum=hoststuff.substring(finalColon+1);

			addrs.add(new InetSocketAddress(hostPart,
					Integer.parseInt(portNum)));
		}
		assert !addrs.isEmpty() : "No addrs found";
		return addrs;
	}

	public static List<InetSocketAddress> getAddresses(List<String> servers) {
		ArrayList<InetSocketAddress> addrs = new ArrayList<InetSocketAddress>(servers.size());
		for (String server : servers) {
			int finalColon=server.lastIndexOf(':');
			if(finalColon < 1) {
				throw new IllegalArgumentException("Invalid server ``"
				+ server + "'' in list:  " + server);
			}
		String hostPart=server.substring(0, finalColon);
		String portNum=server.substring(finalColon+1);

		addrs.add(new InetSocketAddress(hostPart, Integer.parseInt(portNum)));
		}
		if (addrs.isEmpty()) {
		    throw new IllegalArgumentException("servers cannot be empty"); //servers was passed in empty, and shouldn't have been
		}
		return addrs;

	}
}
