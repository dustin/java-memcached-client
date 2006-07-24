// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: A6F886D7-2714-466B-9442-809E094E9790

package net.spy.memcached;

import net.spy.SpyObject;

/**
 * Simple transcoder that just works on strings.
 */
public class StringTranscoder extends SpyObject implements Transcoder {

	public Object decode(CachedData d) {
		assert d.getFlags() == 0;
		return new String(d.getData());
	}

	public CachedData encode(Object o) {
		return new CachedData(0, String.valueOf(o).getBytes());
	}

}
