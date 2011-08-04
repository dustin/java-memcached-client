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

package net.spy.memcached.couch;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import net.spy.memcached.CouchbaseClient;
import net.spy.memcached.couch.TestOperation.TestCallback;
import net.spy.memcached.internal.HttpFuture;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.protocol.couch.HttpOperationImpl;

import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;

/**
 * A TestingClient.
 */
public class TestingClient extends CouchbaseClient {

  public TestingClient(List<URI> baseList, String bucketName, String pwd)
    throws IOException {
    super(baseList, bucketName, pwd);
  }

  public HttpFuture<String> asyncHttpPut(String uri, String document)
    throws UnsupportedEncodingException {
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<String> crv = new HttpFuture<String>(couchLatch,
        operationTimeout);

    HttpRequest request = new BasicHttpEntityEnclosingRequest("PUT", uri,
        HttpVersion.HTTP_1_1);
    StringEntity entity = new StringEntity(document);
    ((BasicHttpEntityEnclosingRequest) request).setEntity(entity);
    HttpOperationImpl op = new TestOperationImpl(request, new TestCallback() {
      private String json;

      @Override
      public void receivedStatus(OperationStatus status) {
        crv.set(json, status);
      }

      @Override
      public void complete() {
        couchLatch.countDown();
      }

      @Override
      public void getData(String response) {
        json = response;
      }
    });
    crv.setOperation(op);
    addOp(op);
    return crv;
  }

  public HttpFuture<String> asyncHttpGet(String uri)
    throws UnsupportedEncodingException {
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<String> crv = new HttpFuture<String>(couchLatch,
        operationTimeout);

    HttpRequest request = new BasicHttpRequest("GET", uri,
        HttpVersion.HTTP_1_1);
    HttpOperationImpl op = new TestOperationImpl(request, new TestCallback() {
      private String json;

      @Override
      public void receivedStatus(OperationStatus status) {
        crv.set(json, status);
      }

      @Override
      public void complete() {
        couchLatch.countDown();
      }

      @Override
      public void getData(String response) {
        json = response;
      }
    });
    crv.setOperation(op);
    addOp(op);
    return crv;
  }

  public HttpFuture<String> asyncHttpDelete(String uri)
    throws UnsupportedEncodingException {
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<String> crv = new HttpFuture<String>(couchLatch,
        operationTimeout);

    HttpRequest request = new BasicHttpRequest("DELETE", uri,
        HttpVersion.HTTP_1_1);
    HttpOperationImpl op = new TestOperationImpl(request, new TestCallback() {
      private String json;

      @Override
      public void receivedStatus(OperationStatus status) {
        crv.set(json, status);
      }

      @Override
      public void complete() {
        couchLatch.countDown();
      }

      @Override
      public void getData(String response) {
        json = response;
      }
    });
    crv.setOperation(op);
    addOp(op);
    return crv;
  }
}
