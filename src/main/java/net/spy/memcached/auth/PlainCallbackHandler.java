package net.spy.memcached.auth;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * Callback handler for doing plain auth.
 */
public class PlainCallbackHandler implements CallbackHandler {

	private final String username;
	private final char[] password;

	/**
	 * Construct a plain callback handler with the given username and password.
	 *
	 * @param u the username
	 * @param p the password
	 */
	public PlainCallbackHandler(String u, String p) {
		username=u;
		password=p.toCharArray();
	}

	public void handle(Callback[] callbacks) throws IOException,
			UnsupportedCallbackException {
		for(Callback cb : callbacks) {
			if(cb instanceof TextOutputCallback) {
				// Not implementing this one yet...
			} else if(cb instanceof NameCallback) {
				NameCallback nc=(NameCallback)cb;
				nc.setName(username);
			} else if(cb instanceof PasswordCallback) {
				PasswordCallback pc=(PasswordCallback)cb;
				pc.setPassword(password);
			} else {
				throw new UnsupportedCallbackException(cb);
			}
		}
	}

}
