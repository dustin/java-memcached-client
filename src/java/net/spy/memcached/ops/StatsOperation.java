package net.spy.memcached.ops;


/**
 * Stats fetching operation.
 */
public interface StatsOperation extends Operation {

	/**
	 * Callback for stats operation.
	 */
	public interface Callback extends OperationCallback {
		/**
		 * Invoked once for every stat returned from the server.
		 *
		 * @param name the name of the stat
		 * @param val the stat value.
		 */
		void gotStat(String name, String val);
	}
	// nothing
}