package net.spy.memcached;

import net.spy.memcached.util.AddrUtil;

/**
 * Binary IPv6 client test.
 */
public class BinaryIPV6ClientTest extends BinaryClientTest {

	@Override
	protected void initClient(ConnectionFactory cf) throws Exception {
		client=new MemcachedClientImpl(cf,
			AddrUtil.getAddresses("::1:11211"));
	}

	@Override
	protected String getExpectedVersionSource() {
		return "/0:0:0:0:0:0:0:1:11211";
	}

}
