package net.spy.memcached.keytransformers;

/**
 * Created by IntelliJ IDEA.
 * User: adMarketplace
 * Date: 6/14/12
 * Time: 2:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class IdentityKeyTransformer implements KeyTransformer {
    public String transform(String key) {
        return key;
    }
}
