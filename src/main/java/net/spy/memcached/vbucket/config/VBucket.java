package net.spy.memcached.vbucket.config;


public class VBucket {

    public final static int MAX_REPLICAS = 4;

    public final static int MAX_BUCKETS = 65536;

    private volatile int master;

    private final int[] replicas;

    public VBucket(int m, int[] r) {
        master = m;
        replicas = r.clone();
    }

    public int getMaster() {
        return master;
    }

    public int getReplica(int n) {
        return replicas[n];
    }

    public void setMaster(int rv) {
        master = rv;
    }
}
