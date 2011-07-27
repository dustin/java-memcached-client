package net.spy.memcached.protocol.couchdb;

public class RowError {
	private final String from;
	private final String reason;

	public RowError(String from, String reason) {
		this.from = from;
		this.reason = reason;
	}

	public String getFrom() {
		return from;
	}

	public String getReason() {
		return reason;
	}
}
