package net.spy.memcached;

import java.net.InetSocketAddress;
import java.util.List;

import junit.framework.TestCase;

/**
 * Test the AddrUtils stuff.
 */
public class AddrUtilTest extends TestCase {

	public void testSingle() throws Exception {
		List<InetSocketAddress> addrs=
			AddrUtil.getAddresses("www.google.com:80");
		assertEquals(1, addrs.size());
		assertEquals("www.google.com", addrs.get(0).getHostName());
		assertEquals(80, addrs.get(0).getPort());
	}

	public void testTwo() throws Exception {
		List<InetSocketAddress> addrs=
			AddrUtil.getAddresses("www.google.com:80 www.yahoo.com:81");
		assertEquals(2, addrs.size());
		assertEquals("www.google.com", addrs.get(0).getHostName());
		assertEquals(80, addrs.get(0).getPort());
		assertEquals("www.yahoo.com", addrs.get(1).getHostName());
		assertEquals(81, addrs.get(1).getPort());
	}

	public void testBrokenHost() throws Exception {
		String s="www.google.com:80 www.yahoo.com:81:more";
		try {
			List<InetSocketAddress> addrs=AddrUtil.getAddresses(s);
			fail("Expected failure, got " + addrs);
		} catch(NumberFormatException e) {
			e.printStackTrace();
			assertEquals("For input string: \"more\"", e.getMessage());
		}
	}

	public void testBrokenHost2() throws Exception {
		String s="www.google.com:80 www.yahoo.com";
		try {
			List<InetSocketAddress> addrs=AddrUtil.getAddresses(s);
			fail("Expected failure, got " + addrs);
		} catch(IllegalArgumentException e) {
			assertEquals("Invalid server ``www.yahoo.com'' in list:  "
					+ s, e.getMessage());
		}
	}

	public void testBrokenList() throws Exception {
		String s="";
		try {
			List<InetSocketAddress> addrs=AddrUtil.getAddresses(s);
			fail("Expected failure, got " + addrs);
		} catch(IllegalArgumentException e) {
			assertEquals("No hosts in list:  ``''", e.getMessage());
		}
	}

	public void testBrokenList2() throws Exception {
		String s="   ";
		try {
			List<InetSocketAddress> addrs=AddrUtil.getAddresses(s);
			fail("Expected failure, got " + addrs);
		} catch(IllegalArgumentException e) {
			assertEquals("No hosts in list:  ``   ''", e.getMessage());
		}
	}

	public void testNullList() throws Exception {
		String s=null;
		try {
			List<InetSocketAddress> addrs=AddrUtil.getAddresses(s);
			fail("Expected failure, got " + addrs);
		} catch(NullPointerException e) {
			assertEquals("Null host list", e.getMessage());
		}
	}

	public void testIPv6Host() throws Exception {
		List<InetSocketAddress> addrs=
			AddrUtil.getAddresses("::1:80");
		assertEquals(1, addrs.size());
		assertEquals("localhost", addrs.get(0).getHostName());
		assertEquals(80, addrs.get(0).getPort());
	}
}
