package net.spy.memcached.protocol.binary;

import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.SASLStepOperation;

public class SASLStepOperationImpl extends SASLBaseOperationImpl
	implements SASLStepOperation {

	private final static int CMD = 0x22;

	public SASLStepOperationImpl(String[] m, byte[] ch, String s,
			Map<String, ?> p, CallbackHandler h, OperationCallback c) {
		super(CMD, m, ch, s, p, h, c);
	}

	@Override
	protected byte[] buildResponse(SaslClient sc) throws SaslException {
		return sc.evaluateChallenge(challenge);

	}
}
