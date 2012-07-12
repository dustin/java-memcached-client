package net.spy.memcached.keytransformers;

import sun.misc.BASE64Encoder;

import java.nio.charset.Charset;

/**
 * Created by IntelliJ IDEA.
 * User: adMarketplace
 * Date: 6/14/12
 * Time: 3:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class Base64KeyTransformer implements KeyTransformer{
    public String transform(String key) {
        if (key == null) return null;
        try {
            return new BASE64Encoder().encode(key.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
