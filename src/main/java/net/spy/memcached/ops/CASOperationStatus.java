package net.spy.memcached.ops;

import net.spy.memcached.CASResponse;

/**
 * OperationStatus subclass for indicating CAS status.
 */
public class CASOperationStatus extends OperationStatus {

	private final CASResponse casResponse;

	public CASOperationStatus(boolean success, String msg, CASResponse cres) {
		super(success, msg);
		casResponse=cres;
	}

	/**
	 * Get the CAS response indicated here.
	 */
	public CASResponse getCASResponse() {
		return casResponse;
	}

}
