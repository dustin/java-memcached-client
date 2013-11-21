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

package net.spy.memcached.internal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;
import net.spy.memcached.MockMemcachedNode;
import net.spy.memcached.TestConfig;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.protocol.BaseOperationImpl;

/**
 * A CheckedOperationTimeoutExceptionTest.
 */
public class CheckedOperationTimeoutExceptionTest extends TestCase {

  public void testSingleOperation() {
    Operation op = buildOp(TestConfig.PORT_NUMBER);
    assertEquals(CheckedOperationTimeoutException.class.getName()
        + ": test - failing node: " + TestConfig.IPV4_ADDR + ":"
            + TestConfig.PORT_NUMBER,
        new CheckedOperationTimeoutException("test", op).toString());
  }

  public void testNullNode() {
    Operation op = new TestOperation();
    assertEquals(CheckedOperationTimeoutException.class.getName()
        + ": test - failing node: <unknown>",
        new CheckedOperationTimeoutException("test", op).toString());
  }

  public void testNullOperation() {
    assertEquals(CheckedOperationTimeoutException.class.getName()
        + ": test - failing node: <unknown>",
        new CheckedOperationTimeoutException("test", (Operation) null)
        .toString());
  }

  public void testMultipleOperation() {
    Collection<Operation> ops = new ArrayList<Operation>();
    ops.add(buildOp(TestConfig.PORT_NUMBER));
    ops.add(buildOp(64212));
    assertEquals(CheckedOperationTimeoutException.class.getName()
        + ": test - failing nodes: " + TestConfig.IPV4_ADDR + ":"
            + TestConfig.PORT_NUMBER + ", " + TestConfig.IPV4_ADDR
            + ":64212",
        new CheckedOperationTimeoutException("test", ops).toString());
  }

  private TestOperation buildOp(int portNum) {
    TestOperation op = new TestOperation();
    MockMemcachedNode node =
        new MockMemcachedNode(InetSocketAddress.createUnresolved(
          TestConfig.IPV4_ADDR, portNum));
    op.setHandlingNode(node);
    return op;
  }

  static class TestOperation extends BaseOperationImpl implements Operation {

    @Override
    public void initialize() {
      throw new RuntimeException("Not implemented.");
    }

    @Override
    public void readFromBuffer(ByteBuffer data) throws IOException {
      throw new RuntimeException("Not implemented");
    }

    @Override
    public byte[] getErrorMsg() {
      return new byte[] {};
    }
  }
}
