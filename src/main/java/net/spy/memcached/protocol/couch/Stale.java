package net.spy.memcached.protocol.couch;

public enum Stale {
	OK {
	    public String toString() {
	        return "ok";
	    }
	},

	UPDATE_AFTER {
	    public String toString() {
	        return "update_after";
	    }
	}
}
