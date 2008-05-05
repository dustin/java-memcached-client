package net.spy.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.io.MemcachedLowLevelIO;
import net.spy.test.SyncThread;

/**
 * Longer running test case.
 */
public class LongClientTest extends ClientBaseCase {

	public void testParallelGet() throws Throwable {
		// Get a connection with the get optimization disabled.
		client.shutdown();
		initClient(new DefaultConnectionFactory(){
			@Override
			public MemcachedLowLevelIO createConnection(
					List<InetSocketAddress> addrs) throws IOException {
				MemcachedLowLevelIO rv = super.createConnection(addrs);
				rv.setGetOptimization(false);
				return rv;
			}
			@Override
			public long getOperationTimeout() {
				return 15000;
			}
			});

		// Throw in some seed data.
		byte data[]=new byte[32768];
		Random r=new Random();
		r.nextBytes(data);
		final int hashcode=Arrays.hashCode(data);
		final Collection<String> keys=new ArrayList<String>();
		for(int i=0; i<50; i++) {
			client.set("k" + i, 60, data);
			keys.add("k" + i);
		}

		// Make sure it got in.
		client.waitForQueues(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

		int cnt=SyncThread.getDistinctResultCount(25, new Callable<Integer>(){
			public Integer call() throws Exception {
				for(int i=0; i<25; i++) {
					Map<String, Object> m = client.getBulk(keys);
					for(String s : keys) {
						byte b[]=(byte[])m.get(s);
						assert Arrays.hashCode(b) == hashcode
							: "Expected " + hashcode + " was "
								+ Arrays.hashCode(b);
					}
				}
				return hashcode;
			}});
		assertEquals(cnt, 25);
	}
}
