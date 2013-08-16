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

package net.spy.memcached.auth;

import javax.security.auth.callback.CallbackHandler;

/**
 * Information required to specify authentication mechanisms and callbacks.
 */
public class AuthDescriptor {

  private final String[] mechs;
  private final CallbackHandler cbh;
  private int authAttempts;
  private int allowedAuthAttempts;

  /**
   * Request authentication using the given list of mechanisms and callback
   * handler.
   *
   * <p>If the list of mechanisms is empty, then the client will try to
   * fetch a list of supported SASL mechanisms from the server. If this is
   * not supported by the server or a specific method needs to be forced,
   * passing a specific list of mechanisms in will work.</p>
   *
   * <p>For example, if the server would support CRAM-MD5 and PLAIN, the
   * most secure variant (CRAM-MD5) will be chosen by default. If PLAIN
   * should be used instead, passing in new String[] {"PLAIN"} will force
   * the client to use PLAIN.</p>
   *
   * @param m list of mechanisms
   * @param h the callback handler for grabbing credentials and stuff
   */
  public AuthDescriptor(String[] m, CallbackHandler h) {
    mechs = m;
    cbh = h;
    authAttempts = 0;
    String authThreshhold =
        System.getProperty("net.spy.memcached.auth.AuthThreshold");
    if (authThreshhold != null) {
      allowedAuthAttempts = Integer.parseInt(authThreshhold);
    } else {
      allowedAuthAttempts = -1; // auth forever
    }
  }

  /**
   * Get a typical auth descriptor for CRAM-MD5 or PLAIN auth with the given
   * username and password.
   *
   * @param u the username
   * @param p the password
   *
   * @return an AuthDescriptor
   */
  public static AuthDescriptor typical(String u, String p) {
    return new AuthDescriptor(new String[] { "CRAM-MD5", "PLAIN" },
        new PlainCallbackHandler(u, p));
  }

  public boolean authThresholdReached() {
    if (allowedAuthAttempts < 0) {
      return false; // negative value means auth forever
    } else if (authAttempts >= allowedAuthAttempts) {
      return true;
    } else {
      authAttempts++;
      return false;
    }
  }

  public String[] getMechs() {
    return mechs;
  }

  public CallbackHandler getCallback() {
    return cbh;
  }
}
