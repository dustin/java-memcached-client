/**
 * Copyright (C) 2006-2009 Dustin Sallings
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

package net.spy.memcached.transcoders;

import java.util.concurrent.Future;

import junit.framework.TestCase;
import net.spy.memcached.CachedData;

/**
 * Test the transcode service.
 */
public class TranscodeServiceTest extends TestCase {

  private TranscodeService ts = null;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ts = new TranscodeService(false);
  }

  @Override
  protected void tearDown() throws Exception {
    ts.shutdown();
    assertTrue(ts.isShutdown());
    super.tearDown();
  }

  public void testNonExecuting() throws Exception {
    CachedData cd = new CachedData(0, new byte[0], 0);
    Future<String> fs = ts.decode(new TestTranscoder(), cd);
    assertEquals("Stuff!", fs.get());
  }

  public void testExecuting() throws Exception {
    CachedData cd = new CachedData(1, new byte[0], 0);
    Future<String> fs = ts.decode(new TestTranscoder(), cd);
    assertEquals("Stuff!", fs.get());
  }

  private static final class TestTranscoder implements Transcoder<String> {

    public boolean asyncDecode(CachedData d) {
      return d.getFlags() == 1;
    }

    public String decode(CachedData d) {
      return "Stuff!";
    }

    public CachedData encode(String o) {
      throw new RuntimeException("Not invoked.");
    }

    public int getMaxSize() {
      return 5;
    }
  }
}
