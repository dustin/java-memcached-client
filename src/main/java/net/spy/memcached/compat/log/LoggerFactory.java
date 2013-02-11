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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory to get logger instances.
 *
 * The system property <code>net.spy.log.LoggerImpl</code> should point
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

  private static volatile LoggerFactory instance = null;

  private final ConcurrentMap<String, Logger> instances;
  private Constructor<? extends Logger> instanceConstructor;

  /**
   * Get an instance of LoggerFactory.
   */
  private LoggerFactory() {
    super();
    instances = new ConcurrentHashMap<String, Logger>();
  }

  private static void init() {
    if (instance == null) {
      instance = new LoggerFactory();
    }
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
    init();
    return (instance.internalGetLogger(name));
  }

  // Get an instance of Logger from internal mechanisms.
  private Logger internalGetLogger(String name) {
    assert name != null : "Name was null";
    Logger rv = instances.get(name);

    if (rv == null) {
      Logger newLogger = null;
      try {
        newLogger = getNewInstance(name);
      } catch (Exception e) {
        throw new RuntimeException("Problem getting logger", e);
      }
      Logger tmp = instances.putIfAbsent(name, newLogger);
      // Return either the new logger we've just made, or one that was
      // created while we were waiting
      rv = tmp == null ? newLogger : tmp;
    }

    return (rv);
  }

  private Logger getNewInstance(String name) throws InstantiationException,
      IllegalAccessException, InvocationTargetException {

    if (instanceConstructor == null) {
      getConstructor();
    }
    Object[] args = { name };
    Logger rv = instanceConstructor.newInstance(args);

    return (rv);
  }

  // Find the appropriate constructor
  @SuppressWarnings("unchecked")
  private void getConstructor() {
    Class<? extends Logger> c = DefaultLogger.class;
    String className = System.getProperty("net.spy.log.LoggerImpl");

    if (className != null) {
      try {
        c = (Class<? extends Logger>) Class.forName(className);
      } catch (NoClassDefFoundError e) {
        System.err.println("Warning:  " + className
            + " not found while initializing"
            + " net.spy.compat.log.LoggerFactory");
        e.printStackTrace();
        c = DefaultLogger.class;
      } catch (ClassNotFoundException e) {
        System.err.println("Warning:  " + className
            + " not found while initializing"
            + " net.spy.compat.log.LoggerFactory");
        e.printStackTrace();
        c = DefaultLogger.class;
      }
    }

    // Find the best constructor
    try {
      // Try to find a constructor that takes a single string
      Class<?>[] args = { String.class };
      instanceConstructor = c.getConstructor(args);
    } catch (NoSuchMethodException e) {
      try {
        // Try to find an empty constructor
        Class<?>[] args = {};
        instanceConstructor = c.getConstructor(args);
      } catch (NoSuchMethodException e2) {
        System.err.println("Warning:  " + className
            + " has no appropriate constructor, using defaults.");

        // Try to find a constructor that takes a single string
        try {
          Class<?>[] args = { String.class };
          instanceConstructor = DefaultLogger.class.getConstructor(args);
        } catch (NoSuchMethodException e3) {
          // This shouldn't happen.
          throw new NoSuchMethodError("There used to be a constructor that "
              + "takes a single String on " + DefaultLogger.class + ", but I "
              + "can't find one now.");
        } // SOL
      } // No empty constructor
    } // No constructor that takes a string
  } // getConstructor
}
