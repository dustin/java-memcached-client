package net.spy.memcached.vbucket.config;

import java.util.List;

public class ConfigDifference {

    /**
     * List of server names that were added.
     */
    private List<String> serversAdded;

    /**
     * List of server names that were removed.
     */
    private List<String> serversRemoved;

    /**
     * Number of vbuckets that changed. -1 if the total number changed.
     */
    private int vbucketsChanges;

    /**
     * True if the sequence of servers changed.
     */
    private boolean sequenceChanged;

    public List<String> getServersAdded() {
        return serversAdded;
    }

    protected void setServersAdded(List<String> serversAdded) {
        this.serversAdded = serversAdded;
    }

    public List<String> getServersRemoved() {
        return serversRemoved;
    }

    protected void setServersRemoved(List<String> serversRemoved) {
        this.serversRemoved = serversRemoved;
    }

    public int getVbucketsChanges() {
        return vbucketsChanges;
    }

    protected void setVbucketsChanges(int vbucketsChanges) {
        this.vbucketsChanges = vbucketsChanges;
    }

    public boolean isSequenceChanged() {
        return sequenceChanged;
    }

    protected void setSequenceChanged(boolean sequenceChanged) {
        this.sequenceChanged = sequenceChanged;
    }
}
