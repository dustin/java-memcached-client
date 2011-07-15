package net.spy.memcached;

import java.net.InetSocketAddress;

/**
 * Binary IPv6 client test.
 */
public class BinaryIPV6ClientTest extends BinaryClientTest {

	@Override
	protected void initClient(ConnectionFactory cf) throws Exception {
		client=new MemcachedClient(cf,
			AddrUtil.getAddresses(TestConfig.IPV6_ADDR + ":11211"));
	}

	@Override
	protected String getExpectedVersionSource() {
		if (TestConfig.defaultToIPV4()) {
			return String.valueOf(
					new InetSocketAddress(TestConfig.IPV4_ADDR, 11211));
		}
		return String.valueOf(
				new InetSocketAddress(TestConfig.IPV6_ADDR, 11211));
	}

}
