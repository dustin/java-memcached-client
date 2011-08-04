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

/**
 * Abstract mechanism for dealing with logs from various objects.
 *
 * Implementations are expected to have a constructor that takes a single String
 * representing the name of the logging item, or an empty constructor.
 *
 * @see LoggerFactory
 */
public interface Logger {

  /**
   * Get the name of this logger.
   */
  String getName();

  /**
   * True if debug is enabled for this logger.
   *
   * @return true if debug messages would be displayed
   */
  boolean isDebugEnabled();

  /**
   * True if info is enabled for this logger.
   *
   * @return true if info messages would be displayed
   */
  boolean isInfoEnabled();

  /**
   * Log a message at the specified level.
   *
   * @param level the level at which to log
   * @param message the message to log
   * @param exception an exception that caused the message
   */
  void log(Level level, Object message, Throwable exception);

  /**
   * Log a message at the specified level.
   *
   * @param level the level at which to log
   * @param message the message to log
   */
  void log(Level level, Object message);

  /**
   * Log a message at debug level.
   *
   * @param message the message to log
   * @param exception the exception that caused the message to be generated
   */
  void debug(Object message, Throwable exception);

  /**
   * Log a message at debug level.
   *
   * @param message the message to log
   */
  void debug(Object message);

  /**
   * Log a formatted message at debug level.
   *
   * @param message the message to log
   * @param args the arguments for that message
   */
  void debug(String message, Object... args);

  /**
   * Log a message at info level.
   *
   * @param message the message to log
   * @param exception the exception that caused the message to be generated
   */
  void info(Object message, Throwable exception);

  /**
   * Log a message at info level.
   *
   * @param message the message to log
   */
  void info(Object message);

  /**
   * Log a formatted message at info level.
   *
   * @param message the message to log
   * @param args the arguments for that message
   */
  void info(String message, Object... args);

  /**
   * Log a message at warning level.
   *
   * @param message the message to log
   * @param exception the exception that caused the message to be generated
   */
  void warn(Object message, Throwable exception);

  /**
   * Log a message at warning level.
   *
   * @param message the message to log
   */
  void warn(Object message);

  /**
   * Log a formatted message at debug level.
   *
   * @param message the message to log
   * @param args the arguments for that message
   */
  void warn(String message, Object... args);

  /**
   * Log a message at error level.
   *
   * @param message the message to log
   * @param exception the exception that caused the message to be generated
   */
  void error(Object message, Throwable exception);

  /**
   * Log a message at error level.
   *
   * @param message the message to log
   */
  void error(Object message);

  /**
   * Log a formatted message at debug level.
   *
   * @param message the message to log
   * @param args the arguments for that message
   */
  void error(String message, Object... args);

  /**
   * Log a message at fatal level.
   *
   * @param message the message to log
   * @param exception the exception that caused the message to be generated
   */
  void fatal(Object message, Throwable exception);

  /**
   * Log a message at fatal level.
   *
   * @param message the message to log
   */
  void fatal(Object message);

  /**
   * Log a formatted message at debug level.
   *
   * @param message the message to log
   * @param args the arguments for that message
   */
  void fatal(String message, Object... args);
}
