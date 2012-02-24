/**
 * Copyright (C) 2009-2012 Couchbase, Inc.
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

package net.spy.memcached.tapmessage;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.SerializingTranscoder;

/**
 * A representation of a tap stream message sent from a tap stream server.
 */
public class ResponseMessage extends BaseMessage {
  // Offsets are given from the end of the header
  private static final int ENGINE_PRIVATE_OFFSET = 24;
  private static final int FLAGS_OFFSET = 26;
  private static final int TTL_OFFSET = 28;
  private static final int RESERVED1_OFFSET = 29;
  private static final int RESERVED2_OFFSET = 30;
  private static final int RESERVED3_OFFSET = 31;
  private static final int ITEM_FLAGS_OFFSET = 32;
  private static final int ITEM_EXPIRY_OFFSET = 36;
  private static final int KEY_OFFSET = 40;

  private final short engineprivate;
  private final List<TapResponseFlag> flags;
  private final byte ttl;
  private final byte reserved1;
  private final byte reserved2;
  private final byte reserved3;
  private final int itemflags;
  private int itemexpiry;
  private final int vbucketstate;
  private final long checkpoint;
  private final byte[] key;
  private final byte[] value;
  private final byte[] revid;

  /**
   * Creates a ResponseMessage from binary data.
   *
   * @param b The binary data sent from the tap stream server.
   */
  public ResponseMessage(byte[] b) {
    // TODO: This isn't the best way of doing this. In the future
    // this should be split into mutiple classes.
    super(b);
    if (!opcode.equals(TapOpcode.NOOP)) {
      engineprivate = decodeShort(b, ENGINE_PRIVATE_OFFSET);
      flags = TapResponseFlag.getFlags(decodeShort(b, FLAGS_OFFSET));
      ttl = b[TTL_OFFSET];
      reserved1 = b[RESERVED1_OFFSET];
      reserved2 = b[RESERVED2_OFFSET];
      reserved3 = b[RESERVED3_OFFSET];
    } else {
      engineprivate = 0;
      flags = new LinkedList<TapResponseFlag>();
      ttl = 0;
      reserved1 = 0;
      reserved2 = 0;
      reserved3 = 0;
    }

    if (opcode.equals(TapOpcode.MUTATION)) {
      if (flags.contains(TapResponseFlag.TAP_FLAG_NETWORK_BYTE_ORDER)) {
        itemflags = decodeInt(b, ITEM_FLAGS_OFFSET);
      } else {
        // handles Couchbase bug MB-4834
        itemflags = decodeIntHostOrder(b, ITEM_FLAGS_OFFSET);
      }
      itemexpiry = decodeInt(b, ITEM_EXPIRY_OFFSET);
      vbucketstate = 0;
      checkpoint = 0;
      revid = new byte[engineprivate];
      System.arraycopy(b, KEY_OFFSET, revid, 0, engineprivate);
      key = new byte[keylength];
      System.arraycopy(b, KEY_OFFSET + engineprivate, key, 0, keylength);
      value = new byte[b.length - keylength - engineprivate - KEY_OFFSET];
      System.arraycopy(b, (b.length - value.length), value, 0, value.length);
    } else if (opcode.equals(TapOpcode.DELETE)) {
      itemflags = 0;
      itemexpiry = 0;
      vbucketstate = 0;
      revid = new byte[engineprivate];
      System.arraycopy(b, 32, revid, 0, engineprivate);
      checkpoint = 0;
      key = new byte[keylength];
      System.arraycopy(b, 32 + engineprivate, key, 0, keylength);
      value = new byte[0];
    } else if (opcode.equals(TapOpcode.VBUCKETSET)) {
      itemflags = 0;
      itemexpiry = 0;
      vbucketstate = decodeInt(b, ITEM_FLAGS_OFFSET);
      checkpoint = 0;
      key = new byte[0];
      value = new byte[0];
      revid = new byte[0];
    } else if (opcode.equals(TapOpcode.START_CHECKPOINT)
      || opcode.equals(TapOpcode.END_CHECKPOINT)) {
      itemflags = 0;
      itemexpiry = 0;
      vbucketstate = 0;
      checkpoint = decodeLong(b, KEY_OFFSET);
      key = new byte[0];
      value = new byte[0];
      revid = new byte[0];
    } else if (opcode.equals(TapOpcode.OPAQUE)) {
      itemflags = 0;
      itemexpiry = 0;
      vbucketstate = decodeInt(b, ITEM_FLAGS_OFFSET);
      checkpoint = 0;
      key = new byte[0];
      value = new byte[0];
      revid = new byte[0];
    } else {
      itemflags = 0;
      itemexpiry = 0;
      vbucketstate = 0;
      checkpoint = 0;
      key = new byte[0];
      value = new byte[0];
      revid = new byte[0];
    }
  }

  /**
   * Gets the value of the engine private field. Not returned in a no-op
   * message.
   *
   * @return The engine private data.
   */
  public long getEnginePrivate() {
    return engineprivate;
  }

  /**
   * Gets the value of the flags field. Not returned in a no-op message.
   *
   * @return The flags data.
   */
  public List<TapResponseFlag> getFlags() {
    return flags;
  }

  /**
   * Gets the value of the time to live field. Not returned in a no-op message.
   *
   * @return The time to live value;
   */
  public int getTTL() {
    return ttl;
  }

  /**
   * Gets the value of the reserved1 field. Not returned in a no-op message.
   *
   * @return The reserved1 data.
   */
  protected int getReserved1() {
    return reserved1;
  }

  /**
   * Gets the value of the reserved2 field. Not returned in a no-op message.
   *
   * @return The reserved2 data.
   */
  protected int getReserved2() {
    return reserved2;
  }

  /**
   * Gets the value of the reserved3 field. Not returned in a no-op message.
   *
   * @return The reserved3 data.
   */
  protected int getReserved3() {
    return reserved3;
  }

  /**
   * Gets the state of the vbucket. Only returned with a tap vbucket state
   * message.
   *
   * @return the vbucket state
   */
  public int getVBucketState() {
    return vbucketstate;
  }

  /**
   * Gets the checkpoint of the vbucket.  Only returned with a start/end
   * checkpoint message.
   *
   * @return the checkpoint
   */
  public long getCheckpoint() {
    return checkpoint;
  }

  /**
   * Gets the value of the items flag field. Only returned with a tap mutation
   * message.
   *
   * @return The items flag data.
   */
  public int getItemFlags() {
    return itemflags;
  }

  /**
   * Gets the value of the item expiry field. Only returned with a tap mutation
   * message.
   *
   * @return The item expiry data.
   */
  public long getItemExpiry() {
    return itemexpiry;
  }

  /**
   * Gets the value of the key field. Only returned with a tap mutation
   * or tap delete message.
   *
   * @return The key data.
   */
  public String getKey() {
    return new String(key);
  }

  /**
   * Gets the value of the value field. Only returned with a tap mutation
   * message.
   *
   * @return The value data.
   */
  public byte[] getValue() {
    return value;
  }

  /**
   * Gets the value of the revid field. Only returned with a tap mutation
   * message.
   *
   * @return The revid of the document.
   */
  public byte[] getRevID() {
    return revid;
  }

  public ByteBuffer getBytes() {
    int bufSize = 0;
    bufSize += HEADER_LENGTH;
    if (opcode.equals(TapOpcode.MUTATION)) {
      bufSize += 16;
    }
    bufSize += getTotalbody();

    ByteBuffer bb = ByteBuffer.allocate(bufSize);
    bb.put(magic.getMagic());
    bb.put(opcode.getOpcode());
    bb.putShort(keylength);
    bb.put(extralength);
    bb.put(datatype);
    bb.putShort(vbucket);
    bb.putInt(totalbody);
    bb.putInt(opaque);
    bb.putLong(cas);

    if (opcode.equals(TapOpcode.NOOP)) {
      return bb;
    }

    bb.putShort(engineprivate);

    short flag = 0;
    for (int i = 0; i < flags.size(); i++) {
      flag |= flags.get(i).getFlags();
    }

    bb.putShort(flag);
    bb.put(ttl);
    bb.put(reserved1);
    bb.put(reserved2);
    bb.put(reserved3);

    if (opcode.equals(TapOpcode.MUTATION)) {
      bb.putInt(itemflags);
      bb.putInt(itemexpiry);
      bb.put(revid);
      bb.put(key);
      bb.put(value);
    } else if (opcode.equals(TapOpcode.DELETE)) {
      bb.put(revid);
      bb.put(key);
    } else if (opcode.equals(TapOpcode.VBUCKETSET)) {
      bb.putInt(vbucketstate);
    }
    return bb;
  }

  @Override
  public String toString() {
    return String.format("Key: %s, Flags: %d, TTL: %d, Size: %d\nValue: %s",
      getKey(), getItemFlags(), getTTL(), getValue().length, deserialize());
  }

  /**
   * Attempt to get the object represented by the given serialized bytes.
   */
  private Object deserialize() {
    SerializingTranscoder tc = new SerializingTranscoder();
    CachedData d = new CachedData(this.getItemFlags(), this.getValue(),
      CachedData.MAX_SIZE);
    Object rv = null;
    rv = tc.decode(d);
    return rv;
  }

}
