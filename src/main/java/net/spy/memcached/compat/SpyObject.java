// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.compat;

import net.spy.memcached.compat.log.Logger;
import net.spy.memcached.compat.log.LoggerFactory;


/**
 * Superclass for all Spy Objects.
 */
public class SpyObject extends Object {

	private transient Logger logger=null;

	/**
	 * Get an instance of SpyObject.
	 */
	public SpyObject() {
		super();
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
