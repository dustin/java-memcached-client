package net.spy.memcached.test;

import java.util.Random;
import java.util.concurrent.ExecutionException;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.ops.OperationErrorType;
import net.spy.memcached.ops.OperationException;

/**
 * Verify what happens when the memory is full on the server.
 *
 * This test expects a server running on localhost:
 *
 * memcached -U 11200 -p 11200 -m 32 -M
 */
public class MemoryFullTest {

	public static void main(String args[]) throws Exception {
		// Verify assertions
		try {
			assert false;
			throw new RuntimeException("Assertions not enabled.");
		} catch(AssertionError e) {
			// OK
		}

		MemcachedClient c=new MemcachedClient(
			AddrUtil.getAddresses("localhost:11200"));
		boolean success=false;
		Random r = new Random();
		byte[] somebytes = new byte[71849];
		r.nextBytes(somebytes);
		try {
			for(int i=0; i<100000; i++) {
				c.set("k" + i, 3600, somebytes).get();
			}
		} catch(ExecutionException e) {
			assert e.getCause() instanceof OperationException;
			OperationException oe = (OperationException)e.getCause();
			assert oe.getType() == OperationErrorType.SERVER;
			assert oe.getMessage().equals(
				"SERVER_ERROR out of memory storing object");
			success=true;
		} finally {
			c.shutdown();
		}
		if(success) {
			System.out.println(":) Failed as expected.");
		} else {
			System.out.println(":( Unexpected failure.");
		}
	}

}
