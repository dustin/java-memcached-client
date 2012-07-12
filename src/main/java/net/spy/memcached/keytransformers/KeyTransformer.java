package net.spy.memcached.keytransformers;

/**
 * Created by IntelliJ IDEA.
 * User: adMarketplace
 * Date: 6/14/12
 * Time: 2:12 PM
 * To change this template use File | Settings | File Templates.
 */
public interface KeyTransformer {
    String transform(String key);
}
