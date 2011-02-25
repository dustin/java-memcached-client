package net.spy.memcached.vbucket.config;

import net.spy.memcached.HashAlgorithm;

import java.util.List;

public class CacheConfig implements Config {

    private final HashAlgorithm hashAlgorithm = HashAlgorithm.NATIVE_HASH;

    private int vbucketsCount;

    private final int serversCount;

    private List<String> servers;

    private List<VBucket> vbuckets;

    public CacheConfig(int serversCount) {
        this.serversCount = serversCount;
    }

    public int getReplicasCount() {
        throw new IllegalArgumentException("TODO: refactor this");
    }

    public int getVbucketsCount() {
        throw new IllegalArgumentException("TODO: refactor this");
    }

    public int getServersCount() {
        return serversCount;
    }

    public String getServer(int serverIndex) {
        if (serverIndex > servers.size() - 1) {
            throw new IllegalArgumentException("Server index is out of bounds, index = "
                    + serverIndex + ", servers count = " + servers.size());
        }
        return servers.get(serverIndex);
    }

    public int getVbucketByKey(String key) {
        throw new IllegalArgumentException("TODO: refactor this");
    }

    public int getMaster(int vbucketIndex) {
        throw new IllegalArgumentException("TODO: refactor this");
    }

    public int getReplica(int vbucketIndex, int replicaIndex) {
        throw new IllegalArgumentException("TODO: refactor this");
    }

    public int foundIncorrectMaster(int vbucket, int wrongServer) {
        throw new IllegalArgumentException("TODO: refactor this");
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }

    public void setVbuckets(List<VBucket> vbuckets) {
        this.vbuckets = vbuckets;
    }

    public List<String> getServers() {
        return servers;
    }

    public List<VBucket> getVbuckets() {
        return vbuckets;
    }

    public ConfigDifference compareTo(Config config) {
        ConfigDifference difference = new ConfigDifference();

        // Compute the added and removed servers
        //difference.setServersAdded(new ArrayList<String>(CollectionUtils.subtract(config.getServers(), this.getServers())));
        //difference.setServersRemoved(new ArrayList<String>(CollectionUtils.subtract(this.getServers(), config.getServers())));

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
                vbucketsChanges += (this.getMaster(i) == config.getMaster(i)) ? 0 : 1;
            }
            difference.setVbucketsChanges(vbucketsChanges);
        } else {
            difference.setVbucketsChanges(-1);
        }

        return difference;
    }

    public HashAlgorithm getHashAlgorithm() {
        return hashAlgorithm;
    }

    public ConfigType getConfigType() {
	return ConfigType.CACHE;
    }


}
