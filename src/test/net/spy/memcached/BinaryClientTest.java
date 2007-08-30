package net.spy.memcached;


/**
 * This test assumes a binary server is running on localhost:11212.
 */
public class BinaryClientTest extends ProtocolBaseCase {

	@Override
	protected void initClient() throws Exception {
		initClient(new BinaryConnectionFactory());
	}

	@Override
	protected void initClient(ConnectionFactory cf) throws Exception {
		client=new MemcachedClient(cf,
			AddrUtil.getAddresses("127.0.0.1:11212"));
	}

	@Override
	protected String getExpectedVersionSource() {
		return "/127.0.0.1:11212";
	}
}
