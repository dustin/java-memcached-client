package net.spy.memcached.vbucket.config;

import java.util.List;

import net.spy.memcached.HashAlgorithm;

public class DefaultConfig implements Config {

    private final HashAlgorithm hashAlgorithm;

    private final int vbucketsCount;

    private final int mask;

    private final int serversCount;

    private final int replicasCount;

    private final List<String> servers;

    private final List<VBucket> vbuckets;

    public DefaultConfig(HashAlgorithm hashAlgorithm, int serversCount,
            int replicasCount, int vbucketsCount, List<String> servers,
            List<VBucket> vbuckets) {
        this.hashAlgorithm = hashAlgorithm;
        this.serversCount = serversCount;
        this.replicasCount = replicasCount;
        this.vbucketsCount = vbucketsCount;
        this.mask = vbucketsCount - 1;
        this.servers = servers;
        this.vbuckets = vbuckets;
    }

    @Override
    public int getReplicasCount() {
        return replicasCount;
    }

    @Override
    public int getVbucketsCount() {
        return vbucketsCount;
    }

    @Override
    public int getServersCount() {
        return serversCount;
    }

    @Override
    public String getServer(int serverIndex) {
        return servers.get(serverIndex);
    }

    @Override
    public int getVbucketByKey(String key) {
        int digest = (int)hashAlgorithm.hash(key);
        return digest & mask;
    }

    @Override
    public int getMaster(int vbucketIndex) {
        return vbuckets.get(vbucketIndex).getMaster();
    }

    @Override
    public int getReplica(int vbucketIndex, int replicaIndex) {
        return vbuckets.get(vbucketIndex).getReplica(replicaIndex);
    }

    @Override
    public int foundIncorrectMaster(int vbucket, int wrongServer) {
        int mappedServer = this.vbuckets.get(vbucket).getMaster();
        int rv = mappedServer;
        if (mappedServer == wrongServer) {
            rv = (rv + 1) % this.serversCount;
            this.vbuckets.get(vbucket).setMaster(rv);
        }
        return rv;
    }

    @Override
    public List<String> getServers() {
        return servers;
    }

    @Override
    public List<VBucket> getVbuckets() {
        return vbuckets;
    }

    @Override
    public ConfigDifference compareTo(Config config) {
        ConfigDifference difference = new ConfigDifference();

        // Verify the servers are equal in their positions
        if (this.serversCount == config.getServersCount()) {
            difference.setSequenceChanged(false);
            for (int i = 0; i < this.serversCount; i++) {
                if (!this.getServer(i).equals(config.getServer(i))) {
                    difference.setSequenceChanged(true);
                    break;
                }
            }
        } else {
            // Just say yes
            difference.setSequenceChanged(true);
        }

        // Count the number of vbucket differences
        if (this.vbucketsCount == config.getVbucketsCount()) {
            int vbucketsChanges = 0;
            for (int i = 0; i < this.vbucketsCount; i++) {
                vbucketsChanges += (this.getMaster(i) == config.getMaster(i)) ? 0
                        : 1;
            }
            difference.setVbucketsChanges(vbucketsChanges);
        } else {
            difference.setVbucketsChanges(-1);
        }

        return difference;
    }

    @Override
    public HashAlgorithm getHashAlgorithm() {
        return hashAlgorithm;
    }

    public ConfigType getConfigType() {
	return ConfigType.MEMBASE;
    }

}
