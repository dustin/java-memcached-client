package net.spy.memcached.test;

import java.util.concurrent.TimeUnit;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.OperationTimeoutException;

public class TimeoutTest {

	private static final String VALUE = "some value";
	private static final String KEY = "someKey";

	public static void main(String args[]) throws Exception {
		MemcachedClient c=new MemcachedClient(new DefaultConnectionFactory(){
			@Override
			public long getOperationTimeout() {
				return 1;
			}
		}, AddrUtil.getAddresses("localhost:11211"));
		c.set(KEY, 0, VALUE);
		try {
			for(int i=0; i<1000; i++) {
				c.get(KEY);
			}
			throw new Exception("Didn't get a timeout.");
		} catch(OperationTimeoutException e) {
			System.out.println("Got a timeout.");
		}
		if(VALUE.equals(c.asyncGet(KEY).get(1, TimeUnit.SECONDS))) {
			System.out.println("Got the right value.");
		} else {
			throw new Exception("Didn't get the expected value.");
		}
	}


}
