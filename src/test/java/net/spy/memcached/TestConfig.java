package net.spy.memcached;

public class TestConfig {
	public static final String IPV4_PROP = "server.address_v4";
	public static final String IPV6_PROP = "server.address_v6";
	public static final String TYPE_MEMCACHED = "memcached";
	public static final String TYPE_MEMBASE = "membase";

	public static final String IPV4_ADDR = System.getProperty(IPV4_PROP, "127.0.0.1");
	public static final String IPV6_ADDR = resolveIpv6Addr();
	public static final String TYPE = System.getProperty("server.type", "memcached").toLowerCase();

	private TestConfig(){
		// empty
	}

	private static String resolveIpv6Addr() {
		String ipv6 = System.getProperty(IPV6_PROP, "::1");;
		if (IPV4_ADDR.equals(ipv6)) {
			return "::ffff:" + ipv6;
		}
		return ipv6;
	}

	public static final boolean isMemcached() {
		return TYPE.equals(TYPE_MEMCACHED);
	}

	public static final boolean isMembase() {
		return TYPE.equals(TYPE_MEMBASE);
	}
}
