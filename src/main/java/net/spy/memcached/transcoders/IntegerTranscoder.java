// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.transcoders;

import net.spy.SpyObject;
import net.spy.memcached.CachedData;

/**
 * Transcoder that serializes and unserializes longs.
 */
public final class IntegerTranscoder extends SpyObject
	implements Transcoder<Integer> {

	private static final int flags = SerializingTranscoder.SPECIAL_INT;

	public CachedData encode(java.lang.Integer l) {
		return new CachedData(flags, TranscoderUtils.encodeInt(l));
	}

	public Integer decode(CachedData d) {
		if (flags == d.getFlags()) {
			return TranscoderUtils.decodeInt(d.getData());
		} else {
			return null;
		}
	}

}
