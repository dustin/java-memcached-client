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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory to get logger instances.
 *
 * The system property <code>net.spy.compat.log.LoggerImpl</code> should point
 * to an implementation of net.spy.compat.log.Logger to use.
 *
 * <p>
 * Depending on how and where this was compiled, a sun logger (jdk 1.4) and/or
 * <a href="http://jakarta.apache.org/log4j/docs/">log4j</a> logger
 * implementation may be included. Both are included with the official
 * distribution.
 * </p>
 *
 * @see AbstractLogger
 */
public final class LoggerFactory extends Object {

  private static LoggerFactory instance = new LoggerFactory();

  private final ConcurrentMap<String, Logger> instances;

  /**
   * Get an instance of LoggerFactory.
   */
  private LoggerFactory() {
    super();
    instances = new ConcurrentHashMap<String, Logger>();
  }

  /**
   * Get a logger by class.
   *
   * @param clazz the class for which we want the logger.
   * @return a Logger instance
   */
  public static Logger getLogger(Class<?> clazz) {
    return (getLogger(clazz.getName()));
  }

  /**
   * Get a logger by name.
   *
   * @param name the name for which we want the logger
   * @return a Logger instance
   */
  public static Logger getLogger(String name) {
    if (name == null) {
      throw new NullPointerException("Logger name may not be null.");
    }
    return (instance.internalGetLogger(name));
  }

  // Get an instance of Logger from internal mechanisms.
  private Logger internalGetLogger(String name) {
    assert name != null : "Name was null";
    Logger rv = instances.get(name);

    if (rv == null) {
      final Logger newLogger = new Slf4JLogger(name);
      Logger tmp = instances.putIfAbsent(name, newLogger);
      // Return either the new logger we've just made, or one that was
      // created while we were waiting
      rv = tmp == null ? newLogger : tmp;
    }

    return (rv);
  }
}
