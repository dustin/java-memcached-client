// Copyright (c) 2006 Dustin Sallings <dustin@spy.net<

package net.spy.memcached.compat;

import java.io.Closeable;

import net.spy.memcached.compat.log.Logger;
import net.spy.memcached.compat.log.LoggerFactory;

/**
 * CloseUtil exists to provide a safe means to close anything closeable.
 * This prevents exceptions from being thrown from within finally blocks while
 * still providing logging of exceptions that occur during close.  Exceptions
 * during the close will be logged using the spy logging infrastructure, but
 * will not be propagated up the stack.
 */
public final class CloseUtil {

	private static Logger logger=LoggerFactory.getLogger(CloseUtil.class);

    /**
     * Close a closeable.
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.info("Unable to close %s", closeable, e);
            }
        }
    }

}
