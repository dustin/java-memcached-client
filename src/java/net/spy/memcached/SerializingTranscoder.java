// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 21495D4D-9106-4A3F-AFD0-8D08C18AF3DC

package net.spy.memcached;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.spy.SpyObject;
import net.spy.util.CloseUtil;

/**
 * Transcoder that serializes and compresses objects.
 */
public class SerializingTranscoder extends SpyObject implements Transcoder {

	/**
	 * Default compression threshold value.
	 */
	public static final int DEFAULT_COMPRESSION_THRESHOLD = 16384;

	// General flags
	static final int SERIALIZED=1;
	static final int COMPRESSED=2;

	// Special flags for specially handled types.
	private static final int SPECIAL_MASK=0xff00;
	private static final int SPECIAL_BOOLEAN=(1<<8);
	private static final int SPECIAL_INT=(2<<8);
	private static final int SPECIAL_LONG=(3<<8);
	private static final int SPECIAL_DATE=(4<<8);
	private static final int SPECIAL_BYTE=(5<<8);
	private static final int SPECIAL_FLOAT=(6<<8);
	private static final int SPECIAL_DOUBLE=(7<<8);
	private static final int SPECIAL_BYTEARRAY=(8<<8);

	private int compressionThreshold=DEFAULT_COMPRESSION_THRESHOLD;

	/**
	 * Set the compression threshold to the given number of bytes.  This
	 * transcoder will attempt to compress any data being stored that's larger
	 * than this.
	 *
	 * @param to the number of bytes
	 */
	public void setCompressionThreshold(int to) {
		compressionThreshold=to;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.Transcoder#decode(net.spy.memcached.CachedData)
	 */
	public Object decode(CachedData d) {
		byte[] data=d.getData();
		Object rv=null;
		if((d.getFlags() & COMPRESSED) != 0) {
			data=decompress(d.getData());
		}
		if((d.getFlags() & SERIALIZED) != 0) {
			rv=deserialize(data);
		} else if((d.getFlags() & SPECIAL_MASK) != 0) {
			switch(d.getFlags() & SPECIAL_MASK) {
				case SPECIAL_BOOLEAN:
					rv=Boolean.valueOf(decodeBoolean(data));
					break;
				case SPECIAL_INT:
					rv=new Integer(decodeInt(data));
					break;
				case SPECIAL_LONG:
					rv=new Long(decodeLong(data));
					break;
				case SPECIAL_DATE:
					rv=new Date(decodeLong(data));
					break;
				case SPECIAL_BYTE:
					rv=new Byte(decodeByte(data));
					break;
				case SPECIAL_FLOAT:
					rv=new Float(Float.intBitsToFloat(decodeInt(data)));
					break;
				case SPECIAL_DOUBLE:
					rv=new Double(Double.longBitsToDouble(decodeLong(data)));
					break;
				case SPECIAL_BYTEARRAY:
					rv=data;
					break;
				default: assert false;
			}
		} else {
			rv=new String(data);
		}
		return rv;
	}

	/* (non-Javadoc)
	 * @see net.spy.memcached.Transcoder#encode(java.lang.Object)
	 */
	public CachedData encode(Object o) {
		CachedData rv=null;
		byte[] b=null;
		int flags=0;
		if(o instanceof String) {
			b=((String)o).getBytes();
		} else if(o instanceof Long) {
			b=encodeLong((Long)o);
			flags |= SPECIAL_LONG;
		} else if(o instanceof Integer) {
			b=encodeInt((Integer)o);
			flags |= SPECIAL_INT;
		} else if(o instanceof Boolean) {
			b=encodeBoolean((Boolean)o);
			flags |= SPECIAL_BOOLEAN;
		} else if(o instanceof Date) {
			b=encodeLong(((Date)o).getTime());
			flags |= SPECIAL_DATE;
		} else if(o instanceof Byte) {
			b=encodeByte((Byte)o);
			flags |= SPECIAL_BYTE;
		} else if(o instanceof Float) {
			b=encodeInt(Float.floatToRawIntBits((Float)o));
			flags |= SPECIAL_FLOAT;
		} else if(o instanceof Double) {
			b=encodeLong(Double.doubleToRawLongBits((Double)o));
			flags |= SPECIAL_DOUBLE;
		} else if(o instanceof byte[]) {
			b=(byte[])o;
			flags |= SPECIAL_BYTEARRAY;
		} else {
			b=serialize(o);
			flags |= SERIALIZED;
		}
		if(b != null) {
			if(b.length > compressionThreshold) {
				byte[] compressed=compress(b);
				if(compressed.length < b.length) {
					getLogger().info("Compressed %s from %d to %d",
						o.getClass().getName(), b.length, compressed.length);
					b=compressed;
					flags |= COMPRESSED;
				} else {
					getLogger().info(
						"Compression increased the size of %s from %d to %d",
						o.getClass().getName(), b.length, compressed.length);
				}
			}
			rv=new CachedData(flags, b);
		}
		return rv;
	}

	private byte[] serialize(Object o) {
		assert o != null;
		byte[] rv=null;
		try {
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			ObjectOutputStream os=new ObjectOutputStream(bos);
			os.writeObject(o);
			os.close();
			bos.close();
			rv=bos.toByteArray();
		} catch(IOException e) {
			throw new IllegalArgumentException("Non-serializable object", e);
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
		getLogger().debug("Compressed %d bytes to %d", in.length, rv.length);
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

	private byte[] encodeNum(long l, int maxBytes) {
		byte[] rv=new byte[maxBytes];
		for(int i=0; i<rv.length; i++) {
			int pos=rv.length-i-1;
			rv[pos]=(byte) ((l >> (8 * i)) & 0xff);
		}
		int firstNonZero=0;
		for(;firstNonZero<rv.length && rv[firstNonZero]==0; firstNonZero++) {
			// Just looking for what we can reduce
		}
		if(firstNonZero > 0) {
			byte[] tmp=new byte[rv.length - firstNonZero];
			System.arraycopy(rv, firstNonZero, tmp, 0, rv.length-firstNonZero);
			rv=tmp;
		}
		return rv;
	}

	byte[] encodeLong(long l) {
		return encodeNum(l, 8);
	}

	long decodeLong(byte[] b) {
		long rv=0;
		for(byte i : b) {
			rv = (rv << 8) | (i<0?256+i:i);
		}
		return rv;
	}

	byte[] encodeInt(int in) {
		return encodeNum(in, 4);
	}

	int decodeInt(byte[] in) {
		assert in.length <= 4
			: "Too long to be an int (" + in.length + ") bytes";
		return (int)decodeLong(in);
	}

	byte[] encodeByte(byte in) {
		return new byte[]{in};
	}

	byte decodeByte(byte[] in) {
		assert in.length <= 1 : "Too long for a byte";
		byte rv=0;
		if(in.length == 1) {
			rv=in[0];
		}
		return rv;
	}

	byte[] encodeBoolean(boolean b) {
		byte[] rv=new byte[1];
		rv[0]=(byte)(b?'1':'0');
		return rv;
	}

	boolean decodeBoolean(byte[] in) {
		assert in.length == 1 : "Wrong length for a boolean";
		return in[0] == '1';
	}

}
