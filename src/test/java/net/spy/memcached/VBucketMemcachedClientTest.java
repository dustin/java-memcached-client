package net.spy.memcached;

import junit.framework.TestCase;

import net.spy.memcached.vbucket.ConfigurationException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;
import java.net.URISyntaxException;
import java.net.URI;
import java.io.IOException;

/**
 * @author Alexander Sokolovsky
 */
public class VBucketMemcachedClientTest extends TestCase {
    public void testOps() throws Exception {
        MemcachedClient mc = null;
           try {
            URI base = new URI("http://localhost:8091/pools");
            mc = new MemcachedClient(Arrays.asList(base), "default", "Administrator", "password", true);
        } catch (IOException ex) {
            Logger.getLogger(VBucketMemcachedClientTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ConfigurationException ex) {
            Logger.getLogger(VBucketMemcachedClientTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(VBucketMemcachedClientTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        Integer i;
        for (i = 0; i < 10000; i++) {
            Future result = mc.set("test" + i, 0, i.toString());
        }
        mc.set("hello", 0, "world");
        String result = (String) mc.get("hello");
        assert (result.equals("world"));

        for (i = 0; i < 10000; i++) {
            String res = (String) mc.get("test" + i);
            assert (res.equals(i.toString()));
        }

        mc.shutdown(3, TimeUnit.SECONDS);
    }
}
