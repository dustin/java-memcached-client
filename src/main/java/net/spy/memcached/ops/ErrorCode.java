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

package net.spy.memcached.ops;

/**
 * Contains a list of Memcached and client specific error codes.
 */
public enum ErrorCode {

  /**
   * The operation was successful.
   */
  SUCCESS((short) 0x00),

  /**
   * The key was not found on the server.
   */
  ERR_NOT_FOUND((short) 0x01),

  /**
   * The key already exists on the server.
   */
  ERR_EXISTS((short) 0x02),

  /**
   * The key or value was too big to store on the server.
   */
  ERR_2BIG((short) 0x03),

  /**
   * The operation sent was not a valid operation.
   */
  ERR_INVAL((short) 0x04),

  /**
   * The value sent in this operation was not stored.
   */
  ERR_NOT_STORED((short) 0x05),

  /**
   * The incr/decr value is bad.
   */
  ERR_DELTA_BADVAL((short) 0x06),

  /**
   * This operation was sent to the wrong server.
   */
  ERR_NOT_MY_VBUCKET((short) 0x07),

  /**
   * An authentication error occurred.
   */
  ERR_AUTH_ERROR((short) 0x20),

  /**
   * Authentication can continue.
   */
  ERR_AUTH_CONTINUE((short) 0x21),
  /**
   * The server doesn't recognize this command.
   */
  ERR_UNKNOWN_COMMAND((short) 0x81),

  /**
   * The server is completely out of memory.
   */
  ERR_NO_MEM((short) 0x82),

  /**
   * This operation is not supported by the server.
   */
  ERR_NOT_SUPPORTED((short) 0x83),

  /**
   * An internal error occurred on the server.
   */
  ERR_INTERNAL((short) 0x84),

  /**
   * The server is busy and the operation should be tried again later.
   */
  ERR_BUSY((short) 0x85),

  /**
   * The server is temporarily out of memory.
   */
  ERR_TEMP_FAIL((short) 0x86),

  /**
   * THIS IS A CLIENT SPECIFIC ERROR CODE.
   * The client received an unknown error code from the server.
   */
  UNKNOWN_ERROR((short) 0xF0),

  /**
   * THIS IS A CLIENT SPECIFIC ERROR CODE.
   * The operation was canceled by the client.
   */
  CANCELLED((short) 0xF1),

  /**
   * THIS IS A CLIENT SPECIFIC ERROR CODE.
   * The operation was timed out in the client.
   */
  TIMED_OUT((short) 0xF2),

  /**
   * THIS IS A CLIENT SPECIFIC ERROR CODE.
   * Client threw exception cause operation failure.
   */
  EXCEPTION((short) 0xF3);

  private final short error;

  ErrorCode(short err) {
    error = err;
  }

  public static ErrorCode getErrorCode(short b) {
    if (b == ErrorCode.SUCCESS.error) {
      return ErrorCode.SUCCESS;
    } else if (b == ErrorCode.ERR_NOT_FOUND.error) {
      return ErrorCode.ERR_NOT_FOUND;
    } else if (b == ErrorCode.ERR_EXISTS.error) {
      return ErrorCode.ERR_EXISTS;
    } else if (b == ErrorCode.ERR_2BIG.error) {
      return ErrorCode.ERR_2BIG;
    } else if (b == ErrorCode.ERR_INVAL.error) {
      return ErrorCode.ERR_INVAL;
    } else if (b == ErrorCode.ERR_NOT_STORED.error) {
      return ErrorCode.ERR_NOT_STORED;
    } else if (b == ErrorCode.ERR_DELTA_BADVAL.error) {
      return ErrorCode.ERR_DELTA_BADVAL;
    } else if (b == ErrorCode.ERR_NOT_MY_VBUCKET.error) {
      return ErrorCode.ERR_NOT_MY_VBUCKET;
    } else if (b == ErrorCode.ERR_AUTH_ERROR.error) {
      return ErrorCode.ERR_AUTH_ERROR;
    } else if (b == ErrorCode.ERR_AUTH_CONTINUE.error) {
      return ErrorCode.ERR_AUTH_CONTINUE;
    }  else if (b == ErrorCode.ERR_UNKNOWN_COMMAND.error) {
      return ErrorCode.ERR_UNKNOWN_COMMAND;
    }  else if (b == ErrorCode.ERR_NO_MEM.error) {
      return ErrorCode.ERR_NO_MEM;
    }  else if (b == ErrorCode.ERR_NOT_SUPPORTED.error) {
      return ErrorCode.ERR_NOT_SUPPORTED;
    }  else if (b == ErrorCode.ERR_INTERNAL.error) {
      return ErrorCode.ERR_INTERNAL;
    }  else if (b == ErrorCode.ERR_BUSY.error) {
      return ErrorCode.ERR_BUSY;
    }  else if (b == ErrorCode.ERR_TEMP_FAIL.error) {
      return ErrorCode.ERR_TEMP_FAIL;
    }
    return ErrorCode.UNKNOWN_ERROR;
  }
}
