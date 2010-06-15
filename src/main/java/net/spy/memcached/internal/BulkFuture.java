package net.spy.memcached.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Additional flexibility for asyncGetBulk
 *
 * <p>
 * This interface is now returned from all asyncGetBulk
 * methods. Unlike {@link #get(long, TimeUnit)},
 * {@link #getSome(long, TimeUnit)} does not throw
 * CheckedOperationTimeoutException, thus allowing retrieval
 * of partial results after timeout occurs. This behavior is
 * especially useful in case of large multi gets.
 * </p>
 *
 * @author boris.partensky@gmail.com
 * @param <V>
 *
 */
public interface BulkFuture<V> extends Future<V> {

	/**
	 * @return true if timeout was reached, false otherwise
	 */
	public boolean isTimeout();

    /**
     * Wait for the operation to complete and return results
     *
     * If operation could not complete within specified
     * timeout, partial result is returned. Otherwise, the
     * behavior is identical to {@link #get(long, TimeUnit)}
     *
     *
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
	public V getSome(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException;

}
