package net.spy.memcached.protocol.binary;

import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.SASLAuthOperation;

public class SASLAuthOperationImpl extends OperationImpl
	implements SASLAuthOperation {

	private final static int CMD = 0x21;

	private final String[] mech;
	private final String serverName;
	private final Map<String, ?> props;
	private final CallbackHandler cbh;

	public SASLAuthOperationImpl(String[] m, String s,
			Map<String, ?> p, CallbackHandler h, OperationCallback c) {
		super(CMD, generateOpaque(), c);
		mech=m;
		serverName=s;
		props=p;
		cbh = h;
	}

	@Override
	public void initialize() {
		try {
			SaslClient sc=Sasl.createSaslClient(mech, null,
					"memcached", serverName, props, cbh);

			// Get optional initial response
			byte[] response =
			    (sc.hasInitialResponse()
					? sc.evaluateChallenge(new byte[0])
					: null);

			String mechanism = sc.getMechanismName();

			prepareBuffer(mechanism, 0, response);
		} catch(SaslException e) {
			// XXX:  Probably something saner can be done here.
			throw new RuntimeException("Can't make SASL go.");
		}
	}

	@Override
	protected void decodePayload(byte[] pl) {
		getLogger().debug("Auth response:  %s", new String(pl));
	}

}
