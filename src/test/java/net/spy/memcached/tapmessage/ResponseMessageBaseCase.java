/**
 * Copyright (C) 2009-2012 Couchbase, Inc.
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

package net.spy.memcached.tapmessage;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * Test ResponseMessage decoding from bytes.
 *
 * TODO: Implement revid check
 * TODO: Replace a lot of the checks for zeros
 */
public class ResponseMessageBaseCase {

  protected byte[] responsebytes;
  protected ResponseMessage instance = null;
  protected List<TapResponseFlag> expectedFlags;

  @BeforeClass
  public static void setUpClass() throws Exception {
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
  }

  @Before
  public void setUp() {
    expectedFlags = new LinkedList<TapResponseFlag>();
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of getEnginePrivate method, of class ResponseMessage.
   */
  @Test
  public void testGetEnginePrivate() {
    long expResult = 0L;
    long result = instance.getEnginePrivate();
    assertEquals(expResult, result);
  }

  /**
   * Test of getFlags method, of class ResponseMessage.
   */
  @Test
  public void testGetFlags() {
    List<TapResponseFlag> result = instance.getFlags();

    short expResultVal = 0;
    for (TapResponseFlag flag : expectedFlags) {
      expResultVal = (short)(expResultVal + flag.getFlags());
    }

    short resultVal = 0;
    for (TapResponseFlag flag : result) {
      resultVal = (short)(resultVal + (int)flag.getFlags());
    }

    assertEquals(expResultVal, resultVal);
  }

  /**
   * Test of getTTL method, of class ResponseMessage.
   */
  @Test
  public void testGetTTL() {
    int expResult = 0;
    int result = instance.getTTL();
    assertEquals(expResult, result);
  }

  /**
   * Test of getReserved1 method, of class ResponseMessage.
   */
  @Test
  public void testGetReserved1() {
    int expResult = 0;
    int result = instance.getReserved1();
    assertEquals(expResult, result);
  }

  /**
   * Test of getReserved2 method, of class ResponseMessage.
   */
  @Test
  public void testGetReserved2() {
    int expResult = 0;
    int result = instance.getReserved2();
    assertEquals(expResult, result);
  }

  /**
   * Test of getReserved3 method, of class ResponseMessage.
   */
  @Test
  public void testGetReserved3() {
    int expResult = 0;
    int result = instance.getReserved3();
    assertEquals(expResult, result);
  }

  /**
   * Test of getVBucketState method, of class ResponseMessage.
   */
  @Test
  public void testGetVBucketState() {
    int expResult = 0;
    int result = instance.getVBucketState();
    assertEquals(expResult, result);
  }

  /**
   * Test of getItemFlags method, of class ResponseMessage.
   */
  @Test
  public void testGetItemFlags() {
    int expResult = (int)0x0200;
    int result = instance.getItemFlags();
    assertEquals(expResult, result);
  }

  /**
   * Test of getItemExpiry method, of class ResponseMessage.
   */
  @Test
  public void testGetItemExpiry() {
    long expResult = 0L;
    long result = instance.getItemExpiry();
    assertEquals(expResult, result);
  }

  /**
   * Test of getKey method, of class ResponseMessage.
   */
  @Test
  public void testGetKey() {
    String expResult = "a";
    String result = instance.getKey();
    assertEquals(expResult, result);
  }

  /**
   * Test of getValue method, of class ResponseMessage.
   */
  @Test
  public void testGetValue() {
    ByteBuffer bb = ByteBuffer.allocate(8);
    bb.put(7, (byte)42);
    byte[] expResult = bb.array();
    byte[] result = instance.getValue();
    assertArrayEquals(expResult, result);
  }

  /**
   * Test of getBytes method, of class ResponseMessage.
   */
  @Test
  public void testGetBytes() {
    byte[] result = instance.getBytes().array();
    assertEquals((byte)42, result[result.length-1]);
  }

}
