// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 21495D4D-9106-4A3F-AFD0-8D08C18AF3DC

package net.spy.memcached;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.spy.SpyObject;
import net.spy.util.CloseUtil;

/**
 * Transcoder that serializes and compresses objects.
 */
public class SerializingTranscoder extends SpyObject implements Transcoder {

	public static final int SERIALIZED=1;
	public static final int COMPRESSED=2;

	private int compressionThreshold=16384;

	public SerializingTranscoder() {
		super();
	}

	/**
	 * Set the compression threshold to the given value.
	 */
	public void setCompressionThreshold(int to) {
		compressionThreshold=to;
	}

	public Object decode(CachedData d) {
		byte data[]=d.getData();
		Object rv=null;
		if((d.getFlags() & COMPRESSED) != 0) {
			data=decompress(d.getData());
		}
		if((d.getFlags() & SERIALIZED) != 0) {
			rv=deserialize(data);
		} else {
			rv=new String(data);
		}
		return rv;
	}

	public CachedData encode(Object o) {
		CachedData rv=null;
		byte b[]=null;
		int flags=0;
		if(o instanceof String) {
			b=((String)o).getBytes();
		} else {
			b=serialize(o);
			flags |= SERIALIZED;
		}
		if(b != null) {
			if(b.length > compressionThreshold) {
				b=compress(b);
				flags |= COMPRESSED;
			}
			rv=new CachedData(flags, b);
		}
		return rv;
	}

	private byte[] serialize(Object o) {
		assert o != null;
		byte rv[]=null;
		try {
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			ObjectOutputStream os=new ObjectOutputStream(bos);
			os.writeObject(o);
			os.close();
			bos.close();
			rv=bos.toByteArray();
		} catch(IOException e) {
			getLogger().warn("Caught IOException encoding %s", o, e);
		}
		return rv;
	}

	private Object deserialize(byte[] in) {
		Object rv=null;
		assert in != null;
		try {
			ByteArrayInputStream bis=new ByteArrayInputStream(in);
			ObjectInputStream is=new ObjectInputStream(bis);
			rv=is.readObject();
			is.close();
			bis.close();
		} catch(IOException e) {
			getLogger().warn("Caught IOException decoding %d bytes of data",
					in.length, e);
		} catch (ClassNotFoundException e) {
			getLogger().warn("Caught CNFE decoding %d bytes of data",
					in.length, e);
		}
		return rv;
	}

	private byte[] compress(byte[] in) {
		assert in != null;
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		GZIPOutputStream gz=null;
		try {
			gz = new GZIPOutputStream(bos);
			gz.write(in);
		} catch (IOException e) {
			throw new RuntimeException("IO exception compressing data", e);
		} finally {
			CloseUtil.close(gz);
			CloseUtil.close(bos);
		}
		byte[] rv=bos.toByteArray();
		getLogger().info("Compressed %d bytes to %d", in.length, rv.length);
		return rv;
	}

	private byte[] decompress(byte[] in) {
		assert in != null;
		ByteArrayInputStream bis=new ByteArrayInputStream(in);
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		GZIPInputStream gis;
		try {
			gis = new GZIPInputStream(bis);

			byte[] buf=new byte[8192];
			int r=-1;
			while((r=gis.read(buf)) > 0) {
				bos.write(buf, 0, r);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error decompressing data", e);
		}
		return bos.toByteArray();
	}

}
