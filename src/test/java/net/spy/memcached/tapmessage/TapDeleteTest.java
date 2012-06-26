package net.spy.memcached.tapmessage;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

public class TapDeleteTest extends ResponseMessageBaseCase {
  @Before
  @Override
  public void setUp() {
    expectedFlags = new LinkedList<TapResponseFlag>();
    expectedFlags.add(TapResponseFlag.TAP_FLAG_NETWORK_BYTE_ORDER);

    // see:
    // raw.github.com/
    // membase/memcached/branch-20/include/memcached/protocol_binary.h
    // bin request header + tap delete + a key
    ByteBuffer binReqTapMutation = ByteBuffer.allocate(24+16+1+8);

    // bin request, tap mutation
    binReqTapMutation.put(0, (byte)0x80).put(1, (byte)0x42);
    binReqTapMutation.put(3, (byte)0x01); // key length
    binReqTapMutation.put(5, (byte)0x06); // datatype
    binReqTapMutation.put(11, (byte)0x0d); // body length
    binReqTapMutation.put(25, (byte)0x04); // rev id length
    binReqTapMutation.put(27, (byte)0x04); // TAP_FLAG_NETWORK_BYTE_ORDER; fixed

    // rev id
    binReqTapMutation.put(32, (byte)'a');
    binReqTapMutation.put(33, (byte)'b');
    binReqTapMutation.put(34, (byte)'c');
    binReqTapMutation.put(35, (byte)'d');
    // key
    binReqTapMutation.put(36, (byte)'a');


    responsebytes = binReqTapMutation.array();

    instance = new ResponseMessage(responsebytes);
  }

  @Test
  public void testGetEnginePrivate() {
    long expResult = 4L;
    long result = instance.getEnginePrivate();
    assertEquals(expResult, result);
  }

  @Test
  public void testGetItemFlags() {
    int expResult = (int)0x0000;
    int result = instance.getItemFlags();
    assertEquals(expResult, result);
  }

  @Test
  public void testGetValue() {
    byte[] expResult = new byte[0];
    byte[] result = instance.getValue();
    assertArrayEquals(expResult, result);
  }

  @Test
  public void testRevID() {
    byte[] result = instance.getRevID();
    assertEquals('a', result[0]);
    assertEquals('b', result[1]);
    assertEquals('c', result[2]);
    assertEquals('d', result[3]);
  }

  @Test
  public void testGetBytes() {
    byte[] result = instance.getBytes().array();
    assertEquals('a', result[result.length-1]);
  }
}
