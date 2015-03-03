/**
 * Copyright (C) 2015 Couchbase
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */

package net.spy.memcached;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import junit.framework.TestCase;

/**
 * Test the KetamaNodeKeyFormatter with the currently supported 2 formats used by
 * either spymemcached and libmemcached
 */
public class KetamaNodeKeyFormatterTest extends TestCase {

    private MemcachedNode defaultNode = new MockMemcachedNode(new InetSocketAddress("localhost", 11211));
    private InetAddress ip;
    private MemcachedNode noHostnameNode;
    private MemcachedNode noDefaultPortNode = new MockMemcachedNode(new InetSocketAddress("localhost", 11212));

    public void setUp() throws Exception {
        ip = InetAddress.getByAddress(new byte[]{1, 2, 3, 4});
        noHostnameNode = new MockMemcachedNode(new InetSocketAddress(ip, 11211));
    }

    public void testSpymemcachedFormatIsDefault() throws Exception {
        KetamaNodeKeyFormatter formatter = new KetamaNodeKeyFormatter();
        assertEquals(formatter.getFormat(), KetamaNodeKeyFormatter.Format.SPYMEMCACHED);
    }

    public void testSpymemcachedFormat() throws Exception {
        KetamaNodeKeyFormatter formatter = new KetamaNodeKeyFormatter(KetamaNodeKeyFormatter.Format.SPYMEMCACHED);
        assertEquals("localhost/127.0.0.1:11211-1", formatter.getKeyForNode(defaultNode, 1));
        assertEquals("1.2.3.4:11211-1", formatter.getKeyForNode(noHostnameNode, 1));
        assertEquals("localhost/127.0.0.1:11212-1", formatter.getKeyForNode(noDefaultPortNode, 1));
    }

    public void testLibmemcachedFormat() throws Exception {
        KetamaNodeKeyFormatter formatter = new KetamaNodeKeyFormatter(KetamaNodeKeyFormatter.Format.LIBMEMCACHED);
        assertEquals("localhost-1", formatter.getKeyForNode(defaultNode, 1));
        assertEquals("1.2.3.4-1", formatter.getKeyForNode(noHostnameNode, 1));
        assertEquals("localhost:11212-1", formatter.getKeyForNode(noDefaultPortNode, 1));
    }
}
