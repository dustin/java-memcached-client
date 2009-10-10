package net.spy.memcached.protocol.binary;

import java.util.Map;

import javax.security.auth.callback.CallbackHandler;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.SASLAuthOperation;

public class SASLAuthOperationImpl extends SASLBaseOperationImpl
	implements SASLAuthOperation {

	private final static int CMD = 0x21;

	public SASLAuthOperationImpl(String[] m, String s,
			Map<String, ?> p, CallbackHandler h, OperationCallback c) {
		super(CMD, m, EMPTY_BYTES, s, p, h, c);
	}
}
