package net.spy.memcached.util.twemproxy;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.util.KetamaNodeLocatorConfiguration;

import java.net.SocketAddress;
import java.util.HashMap;

/**
 * Simple implementation of Twemproxy compatible node locator configuration.
 *
 * Twemproxy differs from the default spymemcached configuration by only using the server
 * hostname and not the hostname and port number for building the continuum hash points.
 *
 * The client must provide a mapping between SocketAddresses and hostname as specified in the
 * Twemproxy configuration file for the the host pool.  In the Twemproxy configuration file,
 * hosts are specified as host:port:weight, only the host portion should be used and it must
 * be specified exactly as in the Twemproxy conf file.  All hosts must be weighted equally.
 *
 * @author Tague Griffith
 */
public class SimpleTwemproxyNodeLocatorConfiguration implements KetamaNodeLocatorConfiguration
{
    protected static final int KETAMA_POINTS_PER_SERVER = 160;
    protected HashMap<SocketAddress,String> hostCache;

    public SimpleTwemproxyNodeLocatorConfiguration(HashMap<SocketAddress, String> hostMap) {

        // build a copy of the host cache
        hostCache = new HashMap<SocketAddress, String>(hostMap);
    }

    /**
     * Returns a uniquely identifying key, suitable for hashing by the
     * KetamaNodeLocator algorithm.
     *
     * @param node       The MemcachedNode to use to form the unique identifier
     * @param repetition The repetition number for the particular node in question
     *                   (0 is the first repetition)
     * @return The key that represents the specific repetition of the node
     */
    @Override
    public String getKeyForNode(MemcachedNode node, int repetition) {
        return hostCache.get(node.getSocketAddress()) + "-" + repetition;
    }

    /**
     * Returns the number of discrete hashes that should be defined for each node
     * in the continuum.
     *
     * @return a value greater than 0
     */
    @Override
    public int getNodeRepetitions() {
        return KETAMA_POINTS_PER_SERVER;
    }

}
