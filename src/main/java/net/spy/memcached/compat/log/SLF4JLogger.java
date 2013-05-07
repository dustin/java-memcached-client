/**
 * Copyright (C) 2009-2013 Couchbase, Inc.
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

/**
 * Logging Implementation using the <a href="http://www.slf4j.org/">SLF4J</a>
 * logging facade.
 *
 * <p>Note that by design, the SLF4J facade does not ship with an actual
 * implementation so that it can be chosen during runtime. If you fail to
 * provide a logging implementation during runtime, no log messages will
 * be logged. See the <a href="http://www.slf4j.org/manual.html">SLF4J
 * Manual</a> for more information on how to do this.</p>
 *
 * <p>Since SLF4J does not support a FATAL log level, errors logged at that
 * level get promoted down to ERROR, since this is the highest level
 * available.</p>
 */
public class SLF4JLogger extends AbstractLogger {

  private final org.slf4j.Logger logger;

  /**
   * Get an instance of the SLF4JLogger.
   */
  public SLF4JLogger(String name) {
    super(name);
    logger = org.slf4j.LoggerFactory.getLogger(name);
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  /**
   * Wrapper around SLF4J logger facade.
   *
   * @param level net.spy.compat.log.Level level.
   * @param message object message
   * @param e optional throwable
   */
  @Override
  public void log(Level level, Object message, Throwable e) {
    if(level == null) {
      level = Level.FATAL;
    }

    switch(level) {
    case TRACE:
      logger.trace(message.toString(), e);
      break;
    case DEBUG:
      logger.debug(message.toString(), e);
      break;
    case INFO:
      logger.info(message.toString(), e);
      break;
    case WARN:
      logger.warn(message.toString(), e);
      break;
    case ERROR:
      logger.error(message.toString(), e);
      break;
    case FATAL:
      logger.error(message.toString(), e);
      break;
    default:
      logger.error("Unhandled Logging Level: " + level
        + " with log message: " + message.toString(), e);
    }
  }

}
