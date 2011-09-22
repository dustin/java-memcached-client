package net.spy.memcached;

public class TestConfig {
	public static final String IPV4_PROP = "server.address_v4";
	public static final String IPV6_PROP = "server.address_v6";
	public static final String TYPE_PROP = "server.type";
	public static final String TYPE_MEMCACHED = "memcached";
	public static final String TYPE_MEMBASE = "membase";

	public static final String IPV4_ADDR = System.getProperty(IPV4_PROP, "127.0.0.1");
	public static final String IPV6_ADDR = resolveIpv6Addr();
	public static final String TYPE = System.getProperty(TYPE_PROP, TYPE_MEMCACHED).toLowerCase();

	private TestConfig(){
		// empty
	}

	private static final String resolveIpv6Addr() {
		String ipv6 = System.getProperty(IPV6_PROP, "::1");
		// If the ipv4 address was set but the ipv6 address wasn't then
		// set the ipv6 address to use ipv4.
		if (!IPV4_ADDR.equals("127.0.0.1") && !IPV4_ADDR.equals("localhost")
				&& ipv6.equals("::1")) {
			return "::ffff:" + IPV4_ADDR;
		}
		return ipv6;
	}

	public static final boolean defaultToIPV4() {
		if(("::ffff:" + IPV4_ADDR).equals(IPV6_ADDR)) {
			return true;
		}
		return false;
	}

	public static final boolean isMemcached() {
		return TYPE.equals(TYPE_MEMCACHED);
	}

	public static final boolean isMembase() {
		return TYPE.equals(TYPE_MEMBASE);
	}
}
