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
public enum TapResponseFlag {
  /**
   * This message requires acknowledgment.
   */
  TAP_ACK((short) 0x01),

  /**
   * This message doesn't contain a value.
   */
  TAP_NO_VALUE((short) 0x02),

  /**
   * This message is sent correctly in network byte order.
   */
  TAP_FLAG_NETWORK_BYTE_ORDER((short) 0x04);

  /**
   * The flag value.
   */
  private short flag;

  /**
   * Defines the flag value.
   *
   * @param flag - The new flag value
   */
  TapResponseFlag(short flag) {
    this.flag = flag;
  }

  public static List<TapResponseFlag> getFlags(short f) {
    List<TapResponseFlag> flags = new LinkedList<TapResponseFlag>();
    if ((f & TapResponseFlag.TAP_ACK.flag) != 0) {
      flags.add(TapResponseFlag.TAP_ACK);
    }
    if ((f & TapResponseFlag.TAP_NO_VALUE.flag) != 0) {
      flags.add(TapResponseFlag.TAP_NO_VALUE);
    }
    if ((f & TapResponseFlag.TAP_FLAG_NETWORK_BYTE_ORDER.flag) != 0) {
      flags.add(TapResponseFlag.TAP_FLAG_NETWORK_BYTE_ORDER);
    }
    return flags;
  }

  public short getFlags() {
    return flag;
  }
}
