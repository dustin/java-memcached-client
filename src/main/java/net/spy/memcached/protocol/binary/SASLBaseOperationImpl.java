package net.spy.memcached.protocol.binary;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;

public abstract class SASLBaseOperationImpl extends OperationImpl {

	private static final int SASL_CONTINUE=0x21;

	protected final String[] mech;
	protected final byte[] challenge;
	protected final String serverName;
	protected final Map<String, ?> props;
	protected final CallbackHandler cbh;

	public SASLBaseOperationImpl(int c, String[] m, byte[] ch,
			String s, Map<String, ?> p, CallbackHandler h,
			OperationCallback cb) {
		super(c, generateOpaque(), cb);
		mech = m;
		challenge = ch;
		serverName = s;
		props = p;
		cbh = h;
	}

	@Override
	public void initialize() {
		try {
			SaslClient sc=Sasl.createSaslClient(mech, null,
					"memcached", serverName, props, cbh);

			byte[] response = buildResponse(sc);
			String mechanism = sc.getMechanismName();

			prepareBuffer(mechanism, 0, response);
		} catch(SaslException e) {
			// XXX:  Probably something saner can be done here.
			throw new RuntimeException("Can't make SASL go.", e);
		}
	}

	protected abstract byte[] buildResponse(SaslClient sc) throws SaslException;

	@Override
	protected void decodePayload(byte[] pl) {
		getLogger().debug("Auth response:  %s", new String(pl));
	}

	@Override
	protected void finishedPayload(byte[] pl) throws IOException {
		if (errorCode == SASL_CONTINUE) {
			getCallback().receivedStatus(new OperationStatus(true,
					new String(pl)));
			transitionState(OperationState.COMPLETE);
		} else if(errorCode == 0) {
			getCallback().receivedStatus(new OperationStatus(true, ""));
			transitionState(OperationState.COMPLETE);
		} else {
			super.finishedPayload(pl);
		}
	}

}
