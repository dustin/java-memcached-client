/**
 * Copyright (C) 2009-2011 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */

package net.spy.memcached.protocol.couch;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A Query.
 */
public class Query {
  private static final String DESCENDING = "descending";
  private static final String ENDKEY = "endkey";
  private static final String ENDKEYDOCID = "endkey_docid";
  private static final String GROUP = "group";
  private static final String GROUPLEVEL = "group_level";
  private static final String INCLUSIVEEND = "inclusive_end";
  private static final String KEY = "key";
  private static final String LIMIT = "limit";
  private static final String REDUCE = "reduce";
  private static final String SKIP = "skip";
  private static final String STALE = "stale";
  private static final String STARTKEY = "startkey";
  private static final String STARTKEYDOCID = "startkey_docid";
  private static final String UPDATESEQ = "update_seq";
  private boolean includedocs = false;

  private Map<String, Object> args;

  public Query() {
    args = new HashMap<String, Object>();
  }

  public boolean willReduce() {
    return (args.containsKey(REDUCE))
      ? ((Boolean)args.get(REDUCE)).booleanValue() : false;
  }

  public boolean willIncludeDocs() {
    return includedocs;
  }

  public Query setDescending(boolean descending) {
    args.put(DESCENDING, Boolean.valueOf(descending));
    return this;
  }

  public Query setEndkeyDocID(String endkeydocid) {
    args.put(ENDKEYDOCID, endkeydocid);
    return this;
  }

  public Query setGroup(boolean group) {
    args.put(GROUP, Boolean.valueOf(group));
    return this;
  }

  public Query setGroup(boolean group, int grouplevel) {
    args.put(GROUP, new Boolean(group));
    args.put(GROUPLEVEL, Integer.valueOf((grouplevel)));
    return this;
  }

  public Query setIncludeDocs(boolean includedocs) {
    this.includedocs = includedocs;
    return this;
  }

  public Query setInclusiveEnd(boolean inclusiveend) {
    args.put(INCLUSIVEEND, Boolean.valueOf(inclusiveend));
    return this;
  }

  public Query setKey(String key) {
    args.put(KEY, key);
    return this;
  }

  public Query setLimit(int limit) {
    args.put(LIMIT, Integer.valueOf(limit));
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

  public Query setReduce(boolean reduce) {
    args.put(REDUCE, new Boolean(reduce));
    return this;
  }

  public Query setRangeEnd(String endkey) {
    args.put(ENDKEY, endkey);
    return this;
  }

  public Query setSkip(int docstoskip) {
    args.put(SKIP, Integer.valueOf(docstoskip));
    return this;
  }

  public Query setStale(Stale stale) {
    if (stale == Stale.OK) {
      args.put(STALE, stale);
    } else if (stale == Stale.UPDATE_AFTER) {
      args.put(STALE, stale);
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
    } else if (value instanceof Stale) {
      return key + "=" + ((Stale) value).toString();
    } else {
      return key + "=\"" + value + "\"";
    }
  }
}
