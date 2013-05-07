/**
 * Copyright (C) 2006-2009 Dustin Sallings
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
 * Abstract implementation of Logger providing most of the common framework.
 */
public abstract class AbstractLogger implements Logger {

  private final String name;

  /**
   * Instantiate the abstract logger.
   */
  protected AbstractLogger(String nm) {
    super();
    if (nm == null) {
      throw new NullPointerException("Logger name may not be null.");
    }
    name = nm;
  }

  /**
   * Get the name of this logger.
   */
  public String getName() {
    return (name);
  }

  /**
   * Get the throwable from the last element of this array if it is Throwable,
   * else null.
   */
  public Throwable getThrowable(Object[] args) {
    Throwable rv = null;
    if (args.length > 0) {
      if (args[args.length - 1] instanceof Throwable) {
        rv = (Throwable) args[args.length - 1];
      }
    }
    return rv;
  }

  /**
   * True if debug is enabled for this logger. Default implementation always
   * returns false
   *
   * @return true if debug messages would be displayed
   */
  public abstract boolean isDebugEnabled();

  /**
   * True if debug is enabled for this logger. Default implementation always
   * returns false
   *
   * @return true if info messages would be displayed
   */
  public abstract boolean isInfoEnabled();

  /**
   * Log a message at trace level.
   *
   * @param message the message to log
   * @param exception the exception that caused the message to be generated
   */
  public void trace(Object message, Throwable exception) {
    log(Level.TRACE, message, exception);
  }

  /**
   * Log a formatted message at trace level.
   *
   * @param message the message to log
   * @param args the arguments for that message
   */
  public void trace(String message, Object... args) {
    if (isDebugEnabled()) {
      trace(String.format(message, args), getThrowable(args));
    }
  }

  /**
   * Log a message at trace level.
   *
   * @param message the message to log
   */
  public void trace(Object message) {
    trace(message, null);
  }

  /**
   * Log a message at debug level.
   *
   * @param message the message to log
   * @param exception the exception that caused the message to be generated
   */
  public void debug(Object message, Throwable exception) {
    log(Level.DEBUG, message, exception);
  }

  /**
   * Log a formatted message at debug level.
   *
   * @param message the message to log
   * @param args the arguments for that message
   */
  public void debug(String message, Object... args) {
    if (isDebugEnabled()) {
      debug(String.format(message, args), getThrowable(args));
    }
  }

  /**
   * Log a message at debug level.
   *
   * @param message the message to log
   */
  public void debug(Object message) {
    debug(message, null);
  }

  /**
   * Log a message at info level.
   *
   * @param message the message to log
   * @param exception the exception that caused the message to be generated
   */
  public void info(Object message, Throwable exception) {
    log(Level.INFO, message, exception);
  }

  /**
   * Log a formatted message at info level.
   *
   * @param message the message to log
   * @param args the arguments for that message
   */
  public void info(String message, Object... args) {
    if (isInfoEnabled()) {
      info(String.format(message, args), getThrowable(args));
    }
  }

  /**
   * Log a message at info level.
   *
   * @param message the message to log
   */
  public void info(Object message) {
    info(message, null);
  }

  /**
   * Log a message at warning level.
   *
   * @param message the message to log
   * @param exception the exception that caused the message to be generated
   */
  public void warn(Object message, Throwable exception) {
    log(Level.WARN, message, exception);
  }

  /**
   * Log a formatted message at debug level.
   *
   * @param message the message to log
   * @param args the arguments for that message
   */
  public void warn(String message, Object... args) {
    warn(String.format(message, args), getThrowable(args));
  }

  /**
   * Log a message at warning level.
   *
   * @param message the message to log
   */
  public void warn(Object message) {
    warn(message, null);
  }

  /**
   * Log a message at error level.
   *
   * @param message the message to log
   * @param exception the exception that caused the message to be generated
   */
  public void error(Object message, Throwable exception) {
    log(Level.ERROR, message, exception);
  }

  /**
   * Log a formatted message at debug level.
   *
   * @param message the message to log
   * @param args the arguments for that message
   */
  public void error(String message, Object... args) {
    error(String.format(message, args), getThrowable(args));
  }

  /**
   * Log a message at error level.
   *
   * @param message the message to log
   */
  public void error(Object message) {
    error(message, null);
  }

  /**
   * Log a message at fatal level.
   *
   * @param message the message to log
   * @param exception the exception that caused the message to be generated
   */
  public void fatal(Object message, Throwable exception) {
    log(Level.FATAL, message, exception);
  }

  /**
   * Log a formatted message at debug level.
   *
   * @param message the message to log
   * @param args the arguments for that message
   */
  public void fatal(String message, Object... args) {
    fatal(String.format(message, args), getThrowable(args));
  }

  /**
   * Log a message at fatal level.
   *
   * @param message the message to log
   */
  public void fatal(Object message) {
    fatal(message, null);
  }

  /**
   * Log a message at the given level.
   *
   * @param level the level
   * @param message the message
   */
  public void log(Level level, Object message) {
    log(level, message, null);
  }

  /**
   * Subclasses should implement this method to determine what to do when a
   * client wants to log at a particular level.
   *
   * @param level the level to log at (see the fields of this class)
   * @param message the message to log
   * @param e the exception that caused the message (or null)
   */
  public abstract void log(Level level, Object message, Throwable e);
}
