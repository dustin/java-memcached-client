package net.spy.memcached.vbucket;

import java.net.PasswordAuthentication;

public class PoolAuthenticator extends java.net.Authenticator {

    private final PasswordAuthentication auth;

    public PoolAuthenticator(String username, String password) {
        super();
        if (username == null || password == null) {
            throw new IllegalArgumentException("Username or Password is not defined.");
        } else {
            this.auth = new PasswordAuthentication(username, password.toCharArray());
        }
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        return auth;
    }


}
