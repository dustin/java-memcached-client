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

import java.util.LinkedList;
import java.util.List;

/**
 * The Flag enum contains a list all of the different flags that can be passed
 * in a tap message in the flag field.
 */
public enum TapRequestFlag {
  /**
   * Tap backfill flag definition.
   */
  BACKFILL((int) 0x01),

  /**
   * Tap dump flag definition.
   */
  DUMP((int) 0x02),

  /**
   * Tap list vBuckets flag definition.
   */
  LIST_VBUCKETS((int) 0x04),

  /**
   * Tap take over vBuckets flag definition.
   */
  TAKEOVER_VBUCKETS((int) 0x08),

  /**
   * Tap support acknowledgment flag definition.
   */
  SUPPORT_ACK((int) 0x10),

  /**
   * Tap send keys only flag definition.
   */
  KEYS_ONLY((int) 0x20),

  /**
   * Tap use checkpoints.
   */
  CHECKPOINT((int) 0x40),

  /**
   * Tap request fixed byteorder.
   *
   * Some releases of Couchbase Server (at least through 1.8.0) send data in
   * host byteorder on x86 based systems.  This requests network byte order
   * (fixed).  See MB-4834.
   */
  FIX_BYTEORDER((int) 0x100);

  /**
   * The flag value.
   */
  private int flag;

  /**
   * Defines the flag value.
   *
   * @param flag - The new flag value
   */
  TapRequestFlag(int flag) {
    this.flag = flag;
  }

  public static List<TapRequestFlag> getFlags(int f) {
    List<TapRequestFlag> flags = new LinkedList<TapRequestFlag>();
    if ((f & TapRequestFlag.BACKFILL.flag) != 0) {
      flags.add(TapRequestFlag.BACKFILL);
    }
    if ((f & TapRequestFlag.DUMP.flag) != 0) {
      flags.add(TapRequestFlag.DUMP);
    }
    if ((f & TapRequestFlag.LIST_VBUCKETS.flag) != 0) {
      flags.add(TapRequestFlag.LIST_VBUCKETS);
    }
    if ((f & TapRequestFlag.TAKEOVER_VBUCKETS.flag) != 0) {
      flags.add(TapRequestFlag.TAKEOVER_VBUCKETS);
    }
    if ((f & TapRequestFlag.SUPPORT_ACK.flag) != 0) {
      flags.add(TapRequestFlag.SUPPORT_ACK);
    }
    if ((f & TapRequestFlag.KEYS_ONLY.flag) != 0) {
      flags.add(TapRequestFlag.KEYS_ONLY);
    }
    if ((f & TapRequestFlag.CHECKPOINT.flag) != 0) {
      flags.add(TapRequestFlag.CHECKPOINT);
    }

    return flags;
  }

  public int getFlags() {
    return flag;
  }
}
