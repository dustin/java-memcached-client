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

package net.spy.memcached.test;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.compat.SpyObject;

/**
 * Authentication functional test.
 */
public class AuthTest extends SpyObject implements Runnable {

  private final String username;
  private final String password;
  private MemcachedClient client;

  public AuthTest(String u, String p) {
    username = u;
    password = p;
  }

  public void init() throws Exception {
    client = new MemcachedClient(new ConnectionFactoryBuilder()
        .setProtocol(Protocol.BINARY)
        .setAuthDescriptor(AuthDescriptor.typical(username, password))
        .build(), AddrUtil.getAddresses("localhost:11212"));
  }

  public void shutdown() throws Exception {
    client.shutdown();
  }

  public void run() {
    System.out.println("Available mechs:  " + client.listSaslMechanisms());
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    client.getVersions();
  }

  public static void main(String[] a) throws Exception {
    AuthTest lt = new AuthTest("testuser", "testpass");
    lt.init();
    long start = System.currentTimeMillis();
    try {
      lt.run();
    } finally {
      lt.shutdown();
    }
    long end = System.currentTimeMillis();
    System.out.println("Runtime:  " + (end - start) + "ms");
  }
}
