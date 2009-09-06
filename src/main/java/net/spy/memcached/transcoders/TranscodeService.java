package net.spy.memcached.transcoders;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import net.spy.memcached.CachedData;
import net.spy.memcached.compat.SpyObject;

/**
 * Asynchronous transcoder.
 */
public class TranscodeService extends SpyObject {

	private final ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 10, 60L,
			TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100),
			new ThreadPoolExecutor.DiscardPolicy());

	/**
	 * Perform a decode.
	 */
	public <T> Future<T> decode(final Transcoder<T> tc,
			final CachedData cachedData) {

		assert !pool.isShutdown() : "Pool has already shut down.";

		TranscodeService.Task<T> task = new TranscodeService.Task<T>(
				new Callable<T>() {
					public T call() {
						return tc.decode(cachedData);
					}
				});

		if (tc.asyncDecode(cachedData)) {
			this.pool.execute(task);
		}
		return task;
	}

	/**
	 * Shut down the pool.
	 */
	public void shutdown() {
		pool.shutdown();
	}

	/**
	 * Ask whether this service has been shut down.
	 */
	public boolean isShutdown() {
		return pool.isShutdown();
	}

	private static class Task<T> extends FutureTask<T> {
		private final AtomicBoolean isRunning = new AtomicBoolean(false);

		public Task(Callable<T> callable) {
			super(callable);
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			this.run();
			return super.get();
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException,
				ExecutionException, TimeoutException {
			this.run();
			return super.get(timeout, unit);
		}

		@Override
		public void run() {
			if (this.isRunning.compareAndSet(false, true)) {
				super.run();
			}
		}
	}

}
