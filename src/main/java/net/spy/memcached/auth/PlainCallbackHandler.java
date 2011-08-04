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

package net.spy.memcached.auth;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * Callback handler for doing plain auth.
 */
public class PlainCallbackHandler implements CallbackHandler {

  private final String username;
  private final char[] password;

  /**
   * Construct a plain callback handler with the given username and password.
   *
   * @param u the username
   * @param p the password
   */
  public PlainCallbackHandler(String u, String p) {
    username = u;
    password = p.toCharArray();
  }

  public void handle(Callback[] callbacks) throws IOException,
      UnsupportedCallbackException {
    for (Callback cb : callbacks) {
      if (cb instanceof TextOutputCallback) {
        // TODO: Implement this
        throw new UnsupportedCallbackException(cb);
      } else if (cb instanceof NameCallback) {
        NameCallback nc = (NameCallback) cb;
        nc.setName(username);
      } else if (cb instanceof PasswordCallback) {
        PasswordCallback pc = (PasswordCallback) cb;
        pc.setPassword(password);
      } else {
        throw new UnsupportedCallbackException(cb);
      }
    }
  }
}
