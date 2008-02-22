package net.spy.memcached;

/**
 * A value with a CAS identifier.
 */
public class CASValue {
	private final long cas;
	private final Object value;

	/**
	 * Construct a new CASValue with the given identifer and value.
	 *
	 * @param c the CAS identifier
	 * @param v the value
	 */
	public CASValue(long c, Object v) {
		super();
		cas=c;
		value=v;
	}

	/**
	 * Get the CAS identifier.
	 */
	public long getCas() {
		return cas;
	}

	/**
	 * Get the object value.
	 */
	public Object getValue() {
		return value;
	}

}
