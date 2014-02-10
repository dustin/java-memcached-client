/**
 * Copyright (C) 2006-2009 Dustin Sallings
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

package net.spy.memcached.ops;

/**
 * Represents status and error codes from the binary protocol.
 */
public enum StatusCode {

  SUCCESS,
  ERR_NOT_FOUND,
  ERR_EXISTS,
  ERR_2BIG,
  ERR_INVAL,
  ERR_NOT_STORED,
  ERR_DELTA_BADVAL,
  ERR_NOT_MY_VBUCKET,
  ERR_UNKNOWN_COMMAND,
  ERR_NO_MEM,
  ERR_NOT_SUPPORTED,
  ERR_INTERNAL,
  ERR_BUSY,
  ERR_TEMP_FAIL,
  CANCELLED,
  INTERRUPTED,
  TIMEDOUT,
  ERR_CLIENT;

  public static StatusCode fromBinaryCode(int code) {
    switch(code) {
      case 0x00:
        return SUCCESS;
      case 0x01:
        return ERR_NOT_FOUND;
      case 0x02:
        return ERR_EXISTS;
      case 0x03:
        return ERR_2BIG;
      case 0x04:
        return ERR_INVAL;
      case 0x05:
        return ERR_NOT_STORED;
      case 0x06:
        return ERR_DELTA_BADVAL;
      case 0x07:
        return ERR_NOT_MY_VBUCKET;
      case 0x81:
        return ERR_UNKNOWN_COMMAND;
      case 0x82:
        return ERR_NO_MEM;
      case 0x83:
        return ERR_NOT_SUPPORTED;
      case 0x84:
        return ERR_INTERNAL;
      case 0x85:
        return ERR_BUSY;
      case 0x86:
        return ERR_TEMP_FAIL;
      default:
        return ERR_CLIENT;
    }
  }

  public static StatusCode fromAsciiLine(String line) {
    if (line.equals("STORED") || line.equals("DELETED")) {
      return SUCCESS;
    } else if (line.equals("NOT_STORED")) {
      return ERR_NOT_STORED;
    } else if (line.equals("EXISTS")) {
      return ERR_EXISTS;
    } else if (line.equals("NOT_FOUND")) {
      return ERR_NOT_FOUND;
    } else if (line.equals("ERROR")
      || line.matches("^(CLIENT|SERVER)_ERROR.*")) {
      return ERR_INTERNAL;
    } else {
      return ERR_CLIENT;
    }
  }

}
