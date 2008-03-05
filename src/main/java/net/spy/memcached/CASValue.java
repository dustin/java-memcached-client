package net.spy.memcached;

/**
 * A value with a CAS identifier.
 */
public class CASValue<T> {
	private final long cas;
	private final T value;

	/**
	 * Construct a new CASValue with the given identifer and value.
	 *
	 * @param c the CAS identifier
	 * @param v the value
	 */
	public CASValue(long c, T v) {
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
	public T getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "{CasValue " + cas + "/" + value + "}";
	}

}
