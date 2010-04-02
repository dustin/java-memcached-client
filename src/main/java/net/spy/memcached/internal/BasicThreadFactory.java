package net.spy.memcached.internal;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple thread factory that can set daemon status on threads and give them names.
 */
public class BasicThreadFactory implements ThreadFactory {

	private static final AtomicInteger poolNumber = new AtomicInteger(1);
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;
	private final boolean daemon;

	public BasicThreadFactory(String name, boolean daemon) {
		this.namePrefix = name + "-" + poolNumber.getAndIncrement() + "-";
		this.daemon = daemon;
	}

	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
		t.setDaemon(daemon);
		return t;
	}

}
