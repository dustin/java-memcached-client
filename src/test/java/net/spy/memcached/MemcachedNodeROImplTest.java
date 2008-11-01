package net.spy.memcached;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

/**
 * Test readonliness of the MemcachedNodeROImpl
 */
public class MemcachedNodeROImplTest extends MockObjectTestCase {

	public void testReadOnliness() throws Exception {
		SocketAddress sa=new InetSocketAddress(11211);
		Mock m = mock(MemcachedNode.class, "node");
		MemcachedNodeROImpl node=
			new MemcachedNodeROImpl((MemcachedNode)m.proxy());
		m.expects(once()).method("getSocketAddress").will(returnValue(sa));

		assertSame(sa, node.getSocketAddress());
		assertEquals(m.proxy().toString(), node.toString());

		for(Method meth : MemcachedNode.class.getMethods()) {
			if(meth.getName().equals("toString")) {
				// ok
			} else if(meth.getName().equals("getSocketAddress")) {
				// ok
			} else {
				Object[] args=new Object[meth.getParameterTypes().length];
				fillArgs(meth.getParameterTypes(), args);
				try {
					meth.invoke(node, args);
					fail("Failed to break on " + meth.getName());
				} catch(InvocationTargetException e) {
					assertSame(UnsupportedOperationException.class,
						e.getCause().getClass());
				}
			}
		}
	}

	private void fillArgs(Class<?>[] parameterTypes, Object[] args) {
		int i=0;
		for(Class<?> c : parameterTypes) {
			if(c == Boolean.TYPE) {
				args[i++] = false;
			} else {
				args[i++] = null;
			}
		}
	}

}
