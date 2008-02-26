package net.spy.memcached;

/**
 * Binary IPv6 client test.
 */
public class BinaryIPV6ClientTest extends BinaryClientTest {

	@Override
	protected void initClient(ConnectionFactory cf) throws Exception {
		client=new MemcachedClient(cf,
			AddrUtil.getAddresses("::1:11212"));
	}

	@Override
	protected String getExpectedVersionSource() {
		return "/0:0:0:0:0:0:0:1:11212";
	}

}
