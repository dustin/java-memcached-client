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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A tap request message that is used to start tap streams, perform sasl
 * authentication, and maintain the health of tap streams.
 */
public class RequestMessage extends BaseMessage{
  private boolean hasBackfill;
  private boolean hasVBucketList;
  private boolean hasVBucketCheckpoints;
  private boolean hasFlags;
  private List<TapRequestFlag> flagList;
  private short[] vblist;
  private String name;
  private long backfilldate;
  private Map<Short, Long> vBucketCheckpoints;

  /**
   * Create a tap request message. These messages are used to start tap streams.
   */
  public RequestMessage() {
    flagList = new LinkedList<TapRequestFlag>();
    vblist = new short[0];
    vBucketCheckpoints = new HashMap<Short, Long>();
    name = UUID.randomUUID().toString();
    backfilldate = -1;
    totalbody += name.length();
    keylength = (short) name.length();
  }

  /**
   * Sets the flags for the tap stream. These flags decide what kind of tap
   * stream will be received.
   *
   * @param f The flags to use for this tap stream.
   */
  public void setFlags(TapRequestFlag f) {
    if (!flagList.contains(f)) {
      if (!hasFlags) {
        hasFlags = true;
        extralength += 4;
        totalbody += 4;
      }
      if (f.equals(TapRequestFlag.BACKFILL)) {
        hasBackfill = true;
        totalbody += 8;
      }
      if (f.equals(TapRequestFlag.LIST_VBUCKETS)
        || f.equals(TapRequestFlag.TAKEOVER_VBUCKETS)) {
        hasVBucketList = true;
        totalbody += 2;
      }
      if (f.equals(TapRequestFlag.CHECKPOINT)) {
        hasVBucketCheckpoints = true;
        totalbody += 2;
      }
      flagList.add(f);
    }
  }

  /**
   * Returns the flags for this message.
   *
   * @return An int value of flags set for this tap message.
   */
  public List<TapRequestFlag> getFlags() {
    return flagList;
  }

  /**
   * Stream all keys inserted into the server after a given date.
   *
   * @param date - The date to stream keys from. Null to stream all keys.
   */
  public void setBackfill(long date) {
    backfilldate = date;
  }

  /**
   * Sets a list of vbuckets to stream keys from.
   *
   * @param vbs - A list of vbuckets.
   */
  public void setVbucketlist(short[] vbs) {
    int oldSize = (vblist.length + 1) * 2;
    int newSize = (vbs.length + 1) * 2;
    totalbody += newSize - oldSize;
    vblist = vbs;
  }

  /**
   * Sets a map of vbucket checkpoints.
   *
   * @param vbchkpnts - A map of vbucket checkpoint identifiers
   */
  public void setvBucketCheckpoints(Map<Short, Long> vbchkpnts) {
    int oldSize = (vBucketCheckpoints.size()) * 10;
    int newSize = (vbchkpnts.size()) * 10;
    totalbody += newSize - oldSize;
    vBucketCheckpoints = vbchkpnts;
  }

  /**
   * Sets a name for this tap stream. If the tap stream fails this name can be
   * used to try to restart the tap stream from where it last left off.
   *
   * @param n The name for the tap stream.
   */
  public void setName(String n) {
    if (n.length() > 65535) {
      throw new IllegalArgumentException("Tap name too long");
    }
    totalbody += n.length() - name.length();
    keylength = (short) n.length();
    name = n;
  }

  /**
   * Encodes the message into binary.
   */
  @Override
  public ByteBuffer getBytes() {
    ByteBuffer bb = ByteBuffer.allocate(HEADER_LENGTH + getTotalbody());
    bb.put(magic.getMagic());
    bb.put(opcode.getOpcode());
    bb.putShort(keylength);
    bb.put(extralength);
    bb.put(datatype);
    bb.putShort(vbucket);
    bb.putInt(totalbody);
    bb.putInt(opaque);
    bb.putLong(cas);

    if (hasFlags) {
      int flag = 0;
      for (int i = 0; i < flagList.size(); i++) {
        flag |= flagList.get(i).getFlags();
      }
      bb.putInt(flag);
    }
    bb.put(name.getBytes());
    if (hasBackfill) {
      bb.putLong(backfilldate);
    }
    if (hasVBucketList) {
      bb.putShort((short) vblist.length);
      for (int i = 0; i < vblist.length; i++) {
        bb.putShort(vblist[i]);
      }
    }
    if (hasVBucketCheckpoints) {
      bb.putShort((short)vBucketCheckpoints.size());
      for (Short vBucket : vBucketCheckpoints.keySet()) {
        bb.putShort(vBucket);
        bb.putLong(vBucketCheckpoints.get(vBucket));
      }
    }

    return (ByteBuffer) bb.flip();
  }
}
