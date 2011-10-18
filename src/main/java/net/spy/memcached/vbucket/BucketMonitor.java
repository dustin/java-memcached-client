/**
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

package net.spy.memcached.vbucket;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.text.ParseException;
import java.util.Observable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.spy.memcached.vbucket.config.Bucket;
import net.spy.memcached.vbucket.config.ConfigurationParser;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;

/**
 * The BucketMonitor will open an HTTP comet stream to monitor for changes to
 * the list of nodes. If the list of nodes changes
 */
public class BucketMonitor extends Observable {

  private final URI cometStreamURI;
  private Bucket bucket;
  private final String httpUser;
  private final String httpPass;
  private final ChannelFactory factory;
  private Channel channel;
  private final String host;
  private final int port;
  private ConfigurationParser configParser;
  private BucketUpdateResponseHandler handler;
  private final HttpMessageHeaders headers;
  /**
   * The specification version which this client meets. This will be included in
   * requests to the server.
   */
  public static final String CLIENT_SPEC_VER = "1.0";

  /**
   * @param cometStreamURI the URI which will stream node changes
   * @param bucketname the bucketToMonitor name we are monitoring
   * @param username the username required for HTTP Basic Auth to the restful
   *          service
   * @param password the password required for HTTP Basic Auth to the restful
   *          service
   */
  public BucketMonitor(URI cometStreamURI, String bucketname, String username,
      String password, ConfigurationParser configParser) {
    super();
    if (cometStreamURI == null) {
      throw new IllegalArgumentException("cometStreamURI cannot be NULL");
    }
    String scheme = cometStreamURI.getScheme() == null ? "http"
        : cometStreamURI.getScheme();
    if (!scheme.equals("http")) {
      // an SslHandler is needed in the pipeline
      // System.err.println("Only HTTP is supported.");
      throw new UnsupportedOperationException("Only http is supported.");
    }

    this.cometStreamURI = cometStreamURI;
    this.httpUser = username;
    this.httpPass = password;
    this.configParser = configParser;
    this.host = cometStreamURI.getHost();
    this.port = cometStreamURI.getPort() == -1 ? 80 : cometStreamURI.getPort();
    factory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
      Executors.newCachedThreadPool());
    this.headers = new HttpMessageHeaders();
  }

  /**
   * A strategy that selects and invokes the appropriate setHeader method on
   * the netty HttpHeader class, either setHeader(String, Object) or
   * setHeader(String, String). This indirection is needed as with netty 3.2.0
   * setHeader(String, String) was changed to setHeader(String, Object) and
   * spymemcached users shall be saved from incompatibilities due to an
   * upgrade to the newer netty version. Once netty is upgraded to 3.2.0+ this
   * may strategy can be replaced with a direct invocation of setHeader.
   */
  private static final class HttpMessageHeaders {

    private final Method m;

    private HttpMessageHeaders() {
      this(getHttpMessageHeaderStrategy());
    }

    private HttpMessageHeaders(final Method m) {
      this.m = m;
    }

    private static Method getHttpMessageHeaderStrategy() {
      try {
        return HttpRequest.class.getMethod("setHeader", String.class,
          Object.class);
      } catch (final SecurityException e) {
        throw new RuntimeException(
          "Cannot check method due to security restrictions.", e);
      } catch (final NoSuchMethodException e) {
        try {
          return HttpRequest.class.getMethod("setHeader", String.class,
            String.class);
        } catch (final Exception e1) {
          throw new RuntimeException(
            "No suitable setHeader method found on netty HttpRequest, the "
            + "signature seems to have changed.", e1);
        }
      }
    }

    void setHeader(HttpRequest obj, String name, String value) {
      try {
        m.invoke(obj, name, value);
      } catch (final Exception e) {
        throw new RuntimeException("Could not invoke method " + m
          + " with args '" + name + "' and '" + value + "'.", e);
      }
    }

  }

  public void startMonitor() {
    if (channel != null) {
      Logger.getLogger(BucketMonitor.class.getName()).log(Level.WARNING,
          "Bucket monitor is already started.");
      return;
    }
    createChannel();
    this.handler = channel.getPipeline().get(BucketUpdateResponseHandler.class);
    handler.setBucketMonitor(this);
    HttpRequest request = prepareRequest(cometStreamURI, host);
    channel.write(request);
    try {
      String response = this.handler.getLastResponse();
      logFiner("Getting server list returns this last chunked response:\n"
          + response);
      Bucket bucketToMonitor = this.configParser.parseBucket(response);
      setBucket(bucketToMonitor);
    } catch (ParseException ex) {
      Logger.getLogger(BucketMonitor.class.getName()).log(Level.WARNING,
        "Invalid client configuration received from server. Staying with "
        + "existing configuration.", ex);
      Logger.getLogger(BucketMonitor.class.getName()).log(Level.FINE,
        "Invalid client configuration received:\n" + handler.getLastResponse()
        + "\n");
    }
  }

  protected void createChannel() {
    // Configure the client.
    ClientBootstrap bootstrap = new ClientBootstrap(factory);

    // Set up the event pipeline factory.
    bootstrap.setPipelineFactory(new BucketMonitorPipelineFactory());

    // Start the connection attempt.
    ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));

    // Wait until the connection attempt succeeds or fails.
    channel = future.awaitUninterruptibly().getChannel();
    if (!future.isSuccess()) {
      bootstrap.releaseExternalResources();
      throw new ConnectionException("Could not connect to any pool member.");
    }
    assert (channel != null);
  }

  protected HttpRequest prepareRequest(URI uri, String h) {
    // Send the HTTP request.
    HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
      HttpMethod.GET, uri.toASCIIString());
    headers.setHeader(request, HttpHeaders.Names.HOST, h);
    if (getHttpUser() != null) {
      String basicAuthHeader;
      try {
        basicAuthHeader =
          ConfigurationProviderHTTP.buildAuthHeader(getHttpUser(),
            getHttpPass());
        headers.setHeader(request, HttpHeaders.Names.AUTHORIZATION,
          basicAuthHeader);
      } catch (UnsupportedEncodingException ex) {
        throw new RuntimeException("Could not encode specified credentials"
            + " for HTTP request.", ex);
      }
    }
    headers.setHeader(request, HttpHeaders.Names.CONNECTION,
      HttpHeaders.Values.CLOSE);  // No keep-alives for this
    headers.setHeader(request, HttpHeaders.Names.CACHE_CONTROL,
      HttpHeaders.Values.NO_CACHE);
    headers.setHeader(request, HttpHeaders.Names.ACCEPT, "application/json");
    headers.setHeader(request, HttpHeaders.Names.USER_AGENT,
      "spymemcached vbucket client");
    headers.setHeader(request,
      "X-memcachekv-Store-Client-Specification-Version", CLIENT_SPEC_VER);
    return request;
  }

  /**
   * Update the config if it has changed and notify our observers.
   *
   * @param bucketToMonitor the bucketToMonitor to set
   */
  private void setBucket(Bucket newBucket) {
    if (this.bucket == null || !this.bucket.equals(newBucket)) {
      this.bucket = newBucket;
      setChanged();
      notifyObservers(this.bucket);
    }
  }

  /**
   * @return the httpUser
   */
  public String getHttpUser() {
    return httpUser;
  }

  /**
   * @return the httpPass
   */
  public String getHttpPass() {
    return httpPass;
  }

  private void logFiner(String msg) {
    Logger.getLogger(BucketMonitor.class.getName()).log(Level.FINER, msg);
  }

  /**
   * Shut down the monitor in a graceful way (and release all resources).
   */
  public void shutdown() {
    shutdown(-1, TimeUnit.MILLISECONDS);
  }

  /**
   * Shut down this monitor in a graceful way.
   *
   * @param timeout
   * @param unit
   */
  public void shutdown(long timeout, TimeUnit unit) {
    deleteObservers();
    if (channel != null) {
      channel.close().awaitUninterruptibly(timeout, unit);
    }
    factory.releaseExternalResources();
  }

  protected void invalidate() {
    try {
      String response = handler.getLastResponse();
      Bucket updatedBucket = this.configParser.parseBucket(response);
      setBucket(updatedBucket);
    } catch (ParseException e) {
      Logger.getLogger(BucketMonitor.class.getName()).log(Level.SEVERE,
          "Invalid client configuration received from server. Staying with "
          +  "existing configuration.", e);
    }
  }

  public void setConfigParser(ConfigurationParser newConfigParser) {
    this.configParser = newConfigParser;
  }
}
