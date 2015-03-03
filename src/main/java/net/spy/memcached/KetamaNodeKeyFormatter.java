/**
 * Copyright (C) 2009-2015 Couchbase, Inc.
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

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Known key formats used in Ketama for assigning nodes around the ring
 */

public class KetamaNodeKeyFormatter {

    public enum Format {
        /**
         * SPYMEMCACHED uses the format traditionally used by spymemcached to map
         * nodes to names. The format is HOSTNAME/IP:PORT-ITERATION
         *
         * <p>
         * This default implementation uses the socket-address of the MemcachedNode
         * and concatenates it with a hyphen directly against the repetition number
         * for example a key for a particular server's first repetition may look like:
         * <p>
         *
         * <p>
         * <code>myhost/10.0.2.1-0</code>
         * </p>
         *
         * <p>
         * for the second repetition
         * </p>
         *
         * <p>
         * <code>myhost/10.0.2.1-1</code>
         * </p>
         *
         * <p>
         * for a server where reverse lookups are failing the returned keys may look
         * like
         * </p>
         *
         * <p>
         * <code>/10.0.2.1-0</code> and <code>/10.0.2.1-1</code>
         * </p>
         */
        SPYMEMCACHED,

        /**
         * LIBMEMCACHED uses the format traditionally used by libmemcached to map
         * nodes to names. The format is HOSTNAME:[PORT]-ITERATION the PORT is not
         * part of the node identifier if it is the default memecached port (11211)
         */
        LIBMEMCACHED
    }

    private final Format format;

    public Format getFormat() {
        return format;
    }

    // Carrried over from the DefaultKetamaNodeLocatorConfiguration:
    // Internal lookup map to try to carry forward the optimisation that was
    // previously in KetamaNodeLocator
    private Map<MemcachedNode, String> nodeKeys = new HashMap<MemcachedNode, String>();

    public KetamaNodeKeyFormatter() {
        this(Format.SPYMEMCACHED);
    }

    public KetamaNodeKeyFormatter(Format format) {
        this.format = format;
    }

    /**
     * Returns a uniquely identifying key, suitable for hashing by the
     * KetamaNodeLocator algorithm.
     *
     * @param node The MemcachedNode to use to form the unique identifier
     * @param repetition The repetition number for the particular node in question
     *          (0 is the first repetition)
     * @return The key that represents the specific repetition of the node
     */
    public String getKeyForNode(MemcachedNode node, int repetition) {
        // Carrried over from the DefaultKetamaNodeLocatorConfiguration:
        // Internal Using the internal map retrieve the socket addresses
        // for given nodes.
        // I'm aware that this code is inherently thread-unsafe as
        // I'm using a HashMap implementation of the map, but the worst
        // case ( I believe) is we're slightly in-efficient when
        // a node has never been seen before concurrently on two different
        // threads, so it the socketaddress will be requested multiple times!
        // all other cases should be as fast as possible.
        String nodeKey = nodeKeys.get(node);
        if (nodeKey == null) {
            switch(this.format) {
                case LIBMEMCACHED:
                    InetSocketAddress address = (InetSocketAddress)node.getSocketAddress();
                    nodeKey = address.getHostName();
                    if (address.getPort() != 11211) {
                        nodeKey += ":" + address.getPort();
                    }
                    break;
                case SPYMEMCACHED:
                    nodeKey = String.valueOf(node.getSocketAddress());
                    if (nodeKey.startsWith("/")) {
                        nodeKey = nodeKey.substring(1);
                    }
                    break;
                default:
                    assert false;
            }
            nodeKeys.put(node, nodeKey);
        }
        return nodeKey + "-" + repetition;
    }
}
