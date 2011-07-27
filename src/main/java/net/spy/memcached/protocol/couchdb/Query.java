package net.spy.memcached.protocol.couchdb;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Query {
	private static final String DESCENDING = "descending";
	private static final String ENDKEY = "endkey";
	private static final String ENDKEYDOCID = "endkey_docid";
	private static final String GROUP = "group";
	private static final String GROUPLEVEL = "group_level";
	private static final String INCLUSIVEEND = "inclusive_end";
	private static final String KEY = "key";
	private static final String LIMIT = "limit";
	private static final String SKIP = "skip";
	private static final String STALE = "stale";
	private static final String STARTKEY = "startkey";
	private static final String STARTKEYDOCID = "startkey_docid";
	private static final String UPDATESEQ = "update_seq";

	private Map<String, Object> args;

	public Query() {
		args = new HashMap<String, Object>();
	}

	public Query setDescending(boolean descending) {
		args.put(DESCENDING, new Boolean(descending));
		return this;
	}

	public Query setEndkeyDocID(String endkeydocid) {
		args.put(ENDKEYDOCID, endkeydocid);
		return this;
	}

	public Query setGroup(boolean group) {
		args.put(GROUP, new Boolean(group));
		return this;
	}

	public Query setGroup(boolean group, int grouplevel) {
		args.put(GROUP, Boolean.toString(group));
		args.put(GROUPLEVEL, new Integer(grouplevel));
		return this;
	}

	public Query setInclusiveEnd(boolean inclusiveend) {
		args.put(INCLUSIVEEND, new Boolean(inclusiveend));
		return this;
	}

	public Query setKey(String key) {
		args.put(KEY, key);
		return this;
	}

	public Query setLimit(int limit) {
		args.put(LIMIT, new Integer(limit));
		return this;
	}

	public Query setRange(String startkey, String endkey) {
		args.put(ENDKEY, endkey);
		args.put(STARTKEY, startkey);
		return this;
	}

	public Query setRangeStart(String startkey) {
		args.put(STARTKEY, startkey);
		return this;
	}

	public Query setRangeEnd(String endkey) {
		args.put(ENDKEY, endkey);
		return this;
	}

	public Query setSkip(int docstoskip) {
		args.put(SKIP, new Integer(docstoskip));
		return this;
	}

	public Query setStale(Stale stale) {
		if (stale == Stale.OK) {
			args.put(STALE, "ok");
		} else if (stale == Stale.UPDATE_AFTER) {
			args.put(STALE, "update_after");
		}
		return this;
	}

	public Query setStartkeyDocID(String startkeydocid) {
		args.put(STARTKEYDOCID, startkeydocid);
		return this;
	}

	public Query setUpdateSeq(boolean updateseq) {
		args.put(UPDATESEQ, Boolean.toString(updateseq));
		return this;
	}

	@Override
	public String toString() {
		boolean first = true;
		String result = "";
		for (Entry<String, Object> arg : args.entrySet()) {
			if (first) {
				result += "?" + getArg(arg.getKey(), arg.getValue());
				first = false;
			} else {
				result += "&" + getArg(arg.getKey(), arg.getValue());
			}
		}
		return result;
	}

	private String getArg(String key, Object value) {
		if (value instanceof Boolean) {
			return key + "=" + ((Boolean) value).toString();
		} else if (value instanceof Integer) {
			return key + "=" + ((Integer) value).toString();
		} else {
			return key + "=\"" + value + "\"";
		}
	}
}
