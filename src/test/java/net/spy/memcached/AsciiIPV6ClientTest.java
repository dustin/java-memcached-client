package net.spy.memcached;

import net.spy.memcached.util.AddrUtil;

/**
 * Test the test protocol over IPv6.
 */
public class AsciiIPV6ClientTest extends AsciiClientTest {

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
