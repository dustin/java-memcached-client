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

package net.spy.memcached;

import java.io.PrintWriter;

import net.spy.memcached.tapmessage.BaseMessage;

/**
 * A utility class for printing the bytes of a tap message.
 */
public final class TapMessagePrinter {

  private TapMessagePrinter() {
    // Empty
  }

  /**
   * Prints the message in byte form in a pretty way. This function is mainly
   * used for debugging.\ purposes.
   */
  public static void printMessage(BaseMessage message, PrintWriter p) {
    int colNum = 0;
    byte[] mbytes = message.getBytes().array();
    p.printf("   %5s%5s%5s%5s\n", "0", "1", "2", "3");
    p.print("   ----------------------");
    for (int i = 0; i < mbytes.length; i++) {
      if ((i % 4) == 0) {
        p.printf("\n%3d|", colNum);
        colNum += 4;
      }
      int field = mbytes[i];
      if (field < 0) {
        field = field + 256;
      }
      p.printf("%5x", field);
    }
    p.print("\n\n");
    p.flush();
  }
}
