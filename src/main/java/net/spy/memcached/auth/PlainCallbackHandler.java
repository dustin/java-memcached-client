package net.spy.memcached.auth;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import net.spy.memcached.ops.OperationStatus;

/**
 * Callback handler for doing plain auth.
 */
public class PlainCallbackHandler implements AuthHandlerBridge {

	private final String username;
	private final char[] password;
	private final CountDownLatch latch;
	private final Collection<OperationStatus> statuses;

	/**
	 * Construct a plain callback handler with the given username and
	 * password.
	 *
	 * @param u the username
	 * @param p the password
	 * @param l the countdown latch to notify when an op completes
	 * @param s a collection of statuses to add each result to
	 */
	public PlainCallbackHandler(String u, String p,
			CountDownLatch l,
			Collection<OperationStatus> s) {
		username = u;
		password = p.toCharArray();
		latch = l;
		statuses = s;
	}

	public void handle(Callback[] callbacks) throws IOException,
			UnsupportedCallbackException {
		for(Callback cb : callbacks) {
            if (cb instanceof TextOutputCallback) {
		// Not implementing this one yet...
            } else if (cb instanceof NameCallback) {
		NameCallback nc = (NameCallback)cb;
		nc.setName(username);
            } else if (cb instanceof PasswordCallback) {
		PasswordCallback pc = (PasswordCallback)cb;
		pc.setPassword(password);
            } else {
		throw new UnsupportedCallbackException(cb);
            }
		}
	}

	public void complete() {
		latch.countDown();
	}

	public void receivedStatus(OperationStatus status) {
		statuses.add(status);
	}

}
