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

package net.spy.memcached.tapmessage;

import java.util.LinkedList;
import java.util.List;

/**
 * The Flag enum contains a list all of the different flags that can be passed
 * in a tap message in the flag field.
 */
public enum TapFlag {
  /**
   * Tap backfill flag definition.
   */
  BACKFILL((byte) 0x01),

  /**
   * Tap dump flag definition.
   */
  DUMP((byte) 0x02),

  /**
   * Tap list vBuckets flag definition.
   */
  LIST_VBUCKETS((byte) 0x04),

  /**
   * Tap take over vBuckets flag definition.
   */
  TAKEOVER_VBUCKETS((byte) 0x08),

  /**
   * Tap support acknowledgment flag definition.
   */
  SUPPORT_ACK((byte) 0x10),

  /**
   * Tap send keys only flag definition.
   */
  KEYS_ONLY((byte) 0x20),

  /**
   * Tap use checkpoints.
   */
  CHECKPOINT((byte) 0x40);

  /**
   * The flag value.
   */
  private byte flag;

  /**
   * Defines the flag value.
   *
   * @param flag - The new flag value
   */
  TapFlag(byte flag) {
    this.flag = flag;
  }

  public static List<TapFlag> getFlags(short f) {
    List<TapFlag> flags = new LinkedList<TapFlag>();
    if ((f & TapFlag.BACKFILL.flag) == 1) {
      flags.add(TapFlag.BACKFILL);
    }
    if ((f & TapFlag.DUMP.flag) == 1) {
      flags.add(TapFlag.DUMP);
    }
    if ((f & TapFlag.LIST_VBUCKETS.flag) == 1) {
      flags.add(TapFlag.LIST_VBUCKETS);
    }
    if ((f & TapFlag.TAKEOVER_VBUCKETS.flag) == 1) {
      flags.add(TapFlag.TAKEOVER_VBUCKETS);
    }
    if ((f & TapFlag.SUPPORT_ACK.flag) == 1) {
      flags.add(TapFlag.SUPPORT_ACK);
    }
    if ((f & TapFlag.KEYS_ONLY.flag) == 1) {
      flags.add(TapFlag.KEYS_ONLY);
    }
    if ((f & TapFlag.CHECKPOINT.flag) == 1) {
      flags.add(TapFlag.CHECKPOINT);
    }

    return flags;
  }

  public byte getFlag() {
    return flag;
  }
}
