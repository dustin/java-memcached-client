package net.spy.memcached.vbucket;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import junit.framework.TestCase;

public class PoolAuthenticatorTest extends TestCase {

    public void testPoolAuthenticator() throws Exception {
        final String username = "test_username";
        final String password = "test_password";

        PoolAuthenticator authenticator = new PoolAuthenticator(username, password);
        assertNotNull(authenticator.getPasswordAuthentication());

        try {
            authenticator = new PoolAuthenticator(null, null);
            fail("Username and password must be defined. Exception must be thrown.");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

}
