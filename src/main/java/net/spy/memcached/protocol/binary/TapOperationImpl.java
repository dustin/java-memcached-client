/**
 * Copyright (C) 2006-2009 Dustin Sallings
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

package net.spy.memcached.protocol.binary;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.TapOperation;
import net.spy.memcached.tapmessage.BaseMessage;
import net.spy.memcached.tapmessage.ResponseMessage;
import net.spy.memcached.tapmessage.TapOpcode;
import net.spy.memcached.tapmessage.TapResponseFlag;

/**
 * Abstract implementation of a tap operation.
 */
public abstract class TapOperationImpl extends OperationImpl implements
    TapOperation {

  private int bytesProcessed;
  private int bodylen;
  private byte[] header;
  private byte[] message;

  static final byte CMD = 0;

  protected TapOperationImpl(OperationCallback cb) {
    super(CMD, generateOpaque(), cb);
    this.header = new byte[BaseMessage.HEADER_LENGTH];
    this.message = null;
  }

  public abstract void initialize();

  @Override
  public void readFromBuffer(ByteBuffer data) throws IOException {
    while (data.remaining() > 0) {
      if (bytesProcessed < BaseMessage.HEADER_LENGTH) {
        header[bytesProcessed] = data.get();
        bytesProcessed++;
      } else {
        if (message == null) {
          bodylen = decodeInt(header, 8);
          message = new byte[BaseMessage.HEADER_LENGTH + bodylen];
          System.arraycopy(header, 0, message, 0, BaseMessage.HEADER_LENGTH);
        }

        if (bytesProcessed < message.length) {
          message[bytesProcessed] = data.get();
          bytesProcessed++;
        }
        if (bytesProcessed >= message.length) {
          ResponseMessage response = new ResponseMessage(message);
          for (TapResponseFlag flag : response.getFlags()) {
            if (flag == TapResponseFlag.TAP_ACK) {
              ((Callback) getCallback()).gotAck(getHandlingNode(),
                  response.getOpcode(), response.getOpaque());
            }
          }
          if (response.getOpcode() != TapOpcode.OPAQUE && response.getOpcode()
            != TapOpcode.NOOP) {
            ((Callback) getCallback()).gotData(response);
          }
          message = null;
          bytesProcessed = 0;
        }
      }
    }
  }
}
