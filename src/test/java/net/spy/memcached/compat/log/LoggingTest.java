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

package net.spy.memcached.compat.log;

import junit.framework.TestCase;

// XXX:  This really needs to get log4j configured first.

/**
 * Make sure logging is enabled.
 */
public class LoggingTest extends TestCase {

  private Logger logger = null;

  /**
   * Get an instance of LoggingTest.
   */
  public LoggingTest(String name) {
    super(name);
  }

  /**
   * Set up logging.
   */
  @Override
  public void setUp() {
    logger = LoggerFactory.getLogger(getClass());
  }

  /**
   * Make sure logging is enabled.
   */
  public void testDebugLogging() {
    // assertTrue("Debug logging is not enabled", logger.isDebugEnabled());
    logger.debug("debug message");
  }

  /**
   * Make sure info is enabled, and test it.
   */
  public void testInfoLogging() {
    assertTrue(logger.isInfoEnabled());
    logger.info("info message");
  }

  /**
   * Test other log stuff.
   */
  public void testOtherLogging() {
    logger.warn("warn message");
    logger.warn("test %s", "message");
    logger.error("error message");
    logger.error("test %s", "message");
    logger.fatal("fatal message");
    logger.fatal("test %s", "message");
    assertEquals(getClass().getName(), logger.getName());
  }

  /**
   * Test the slf4j logger.
   */
  public void testSlf4JLogger() {
    Logger l = new Slf4JLogger(getClass().getName());
    assertFalse(l.isDebugEnabled());
    l.debug("debug message");
    assertTrue(l.isInfoEnabled());
    l.info("info message");
    l.warn("warn message");
    l.error("error message");
    l.fatal("fatal message");
    l.fatal("fatal message with exception", new Exception());
  }

  /**
   * Test picking up an exception argument.
   */
  public void testExceptionArg() throws Exception {
    Object[] args = new Object[] { "a", 42, new Exception("test") };
    Throwable t = ((Slf4JLogger) logger).getThrowable(args);
    assertNotNull(t);
    assertEquals("test", t.getMessage());
  }

  /**
   * Test when the last argument is not an exception.
   */
  public void testNoExceptionArg() throws Exception {
    Object[] args = new Object[] { "a", 42, new Exception("test"), "x" };
    Throwable t = ((Slf4JLogger) logger).getThrowable(args);
    assertNull(t);
   }
}
