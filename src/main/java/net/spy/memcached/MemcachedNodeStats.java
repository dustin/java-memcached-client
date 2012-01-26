package net.spy.memcached;

import java.util.concurrent.atomic.AtomicLong;

import org.weakref.jmx.Managed;

public class MemcachedNodeStats
{
    private final AtomicLong inputOpsReadCount = new AtomicLong();
    private final AtomicLong inputOpsWrittenCount = new AtomicLong();

    private final AtomicLong writeOpsReadCount = new AtomicLong();
    private final AtomicLong writeOpsWrittenCount = new AtomicLong();

    private final AtomicLong readOpsReadCount = new AtomicLong();
    private final AtomicLong readOpsWrittenCount = new AtomicLong();

    private final AtomicLong setupResendCount = new AtomicLong();
    private final AtomicLong cancelOpsCount = new AtomicLong();

    private final AtomicLong optimizedGetsCount = new AtomicLong();
    private final AtomicLong optimizedSetsCount = new AtomicLong();

    public long inputOpsRead(final int count)
    {
        return inputOpsReadCount.addAndGet(count);
    }

    public long inputOpsWritten(final int count)
    {
        return inputOpsWrittenCount.addAndGet(count);
    }

    public long writeOpsRead(final int count)
    {
        return writeOpsReadCount.addAndGet(count);
    }

    public long writeOpsWritten(final int count)
    {
        return writeOpsWrittenCount.addAndGet(count);
    }

    public long readOpsRead(final int count)
    {
        return readOpsReadCount.addAndGet(count);
    }

    public long readOpsWritten(final int count)
    {
        return readOpsWrittenCount.addAndGet(count);
    }

    public long setupResend()
    {
        return setupResendCount.incrementAndGet();
    }

    public long cancelOp()
    {
        return cancelOpsCount.incrementAndGet();
    }

    public long optimizedGets(int count)
    {
        return optimizedGetsCount.addAndGet(count);
    }

    public long optimizedSets(int count)
    {
        return optimizedSetsCount.addAndGet(count);
    }

    @Managed(description="number of cancelled operations")
    public long getCancelOps()
    {
        return cancelOpsCount.get();
    }

    @Managed(description="number of ops read from the input queue")
    public long getInputOpsRead()
    {
        return inputOpsReadCount.get();
    }

    @Managed(description="number of ops written in the input queue")
    public long getInputOpsWritten()
    {
        return inputOpsWrittenCount.get();
    }

    @Managed(description="number of ops read from the read queue")
    public long getReadOpsRead()
    {
        return readOpsReadCount.get();
    }

    @Managed(description="number of ops written in the read queue")
    public long getReadOpsWritten()
    {
        return readOpsWrittenCount.get();
    }

    @Managed(description="number of ops resend setups")
    public long getSetupResendCount()
    {
        return setupResendCount.get();
    }

    @Managed(description="number of ops read from the write queue")
    public long getWriteOpsRead()
    {
        return writeOpsReadCount.get();
    }

    @Managed(description="number of ops written in the write queue")
    public long getWriteOpsWritten()
    {
        return writeOpsWrittenCount.get();
    }

    @Managed(description="number of optimized get operations")
    public long getOptimizedGets()
    {
        return optimizedGetsCount.get();
    }

    @Managed(description="number of optimized set operations")
    public long getOptimizedSets()
    {
        return optimizedSetsCount.get();
    }
}
