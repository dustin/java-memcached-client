// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.compat;

import net.spy.memcached.compat.log.Logger;
import net.spy.memcached.compat.log.LoggerFactory;

/**
 * Superclass for all Spy Threads.
 */
public class SpyThread extends Thread {

	private transient Logger logger=null;

	// Thread has *eight* constructors.  Damnit.

	/**
	 * Get an instance of SpyThread.
	 */
	public SpyThread() {
		super();
	}

	/**
	 * Get an instance of SpyThread with a name.
	 *
	 * @param name thread name
	 */
	public SpyThread(String name) {
		super(name);
	}

	/**
	 * Get a Logger instance for this class.
	 *
	 * @return an appropriate logger instance.
	 */
	protected Logger getLogger() {
		if(logger==null) {
			logger=LoggerFactory.getLogger(getClass());
		}
		return(logger);
	}

}
