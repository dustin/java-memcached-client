// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.memcached.transcoders;

import java.io.ObjectStreamClass;

import net.spy.SpyObject;
import net.spy.memcached.CachedData;

/**
 * Transcoder that serializes and unserializes longs.
 */
public final class LongTranscoder extends SpyObject
	implements Transcoder<Long> {

	private static final int flags = TranscoderUtils.hashForFlags(
		ObjectStreamClass.lookup(Long.class).getSerialVersionUID());

	public CachedData encode(java.lang.Long l) {
		return new CachedData(flags, TranscoderUtils.encodeLong(l));
	}

	public Long decode(CachedData d) {
		if (flags == d.getFlags()) {
			return TranscoderUtils.decodeLong(d.getData());
		} else {
			return null;
		}
	}

}
