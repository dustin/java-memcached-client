package net.spy.memcached.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.OperationTimeoutException;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;
import static org.junit.Assert.*;

/**
 * A very simple test of using SASL PLAIN auth and ensuring that operations are
 * not sent without being authenticated, if a MemcachedClient is told 
 * (via the ConnectionFactoryBuilder) that it should be authenticating.
 *
 * @author Matt Ingenthron <ingenthr@cep.net>
 */
public class SASLConnectReconnect {

    private MemcachedClient mc = null;

    SASLConnectReconnect(String username, String password, String host) {

	AuthDescriptor ad = new AuthDescriptor(new String[]{"PLAIN"},
		new PlainCallbackHandler(username, password));
	try {
	    List<InetSocketAddress> addresses = AddrUtil.getAddresses(host);
	    mc = new MemcachedClient(
		    new ConnectionFactoryBuilder().setProtocol(Protocol.BINARY).setAuthDescriptor(ad).build(),
		    addresses);
	} catch (IOException ex) {
	    System.err.println("Couldn't create a connection, bailing out: \nIOException " + ex.getMessage());
	    if (mc != null) {
		mc.shutdown();
	    }
	}




    }

    /**
     *
     * The intent of this test is to verify that if MemcachedClient object is
     * set up for SASL Auth the operations are only sent after SASL auth has
     * been completed, even in a reconnect case.
     *
     * At the moment, I use external start/restart of the memcached and
     * external verification that the behavior was correct.
     *
     * Example arguments for running this test:
     * username password 127.0.0.1:11211 10000
     *
     * The initial run does it's thing, then pauses for 30 seconds, while
     * I bounce the server.  Then it runs the second pass.
     *
     * @param args the command line arguments
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
	if (args.length != 4) {
	    System.err.println("Usage example:\nQuickAuthLoad user password localhost:11212 10000");
	    System.exit(1);
	}
	SASLConnectReconnect m = new SASLConnectReconnect(args[0], args[1], args[2]);

	Logger.getLogger("net.spy.memcached").setLevel(Level.FINEST);

	//get the top Logger, create it if it doesn't exist, set to FINEST
	Logger topLogger = java.util.logging.Logger.getLogger("");

	Handler consoleHandler = null;
	for (Handler handler : topLogger.getHandlers()) {
	    if (handler instanceof ConsoleHandler) {
		consoleHandler = handler;
		break;
	    }
	}

	if (consoleHandler == null) {
	    consoleHandler = new ConsoleHandler();
	    topLogger.addHandler(consoleHandler);
	}
	consoleHandler.setLevel(java.util.logging.Level.FINEST);

	m.verifySetAndGet();
	System.err.println("Pass one done.");
	Thread.sleep(60000);
	m.verifySetAndGet2();
	System.err.println("Pass two done.");

    }

    /**
     * verify set and get go to the right place
     */
    public void verifySetAndGet() {
	int iterations = 20;
	for (int i = 0; i < iterations; i++) {
	    mc.set("test" + i, 0, "test" + i);
	}

	for (int i = 0; i < iterations; i++) {
	    Object res = mc.get("test" + i);
	    assertEquals("test" + i, res);
	}
    }

    /**
     * verify set and get go to the right place
     */
    public void verifySetAndGet2() {
	try {
	    int iterations = 50000;
	    for (int i = 0; i < iterations; i++) {
		mc.set("me" + i, 0, "me" + i);
	    }

	    for (int i = 0; i < iterations; i++) {
		try {
		    Object res = mc.get("me" + i);
		    if (res == null) {
			System.err.println("me" + i + " was not in the cache.");
		    } else {
			assertEquals("me" + i, res);
		    }
		} catch (OperationTimeoutException ex) {
		    System.err.println("Operation timeed out, continuing.");
		}
	    }
	} catch (Exception ex) {
	    System.err.println("Bailing out " + ex.toString() + "\n");
	    ex.printStackTrace();
	}
    }
}


