package net.spy.memcached.util;

import java.util.Collection;
import java.util.LinkedList;

import org.junit.Test;

import junit.framework.TestCase;

public class UtilTest extends TestCase {
  public void setup() {
    // Empty
  }

  public void teardown() {
    // Empty
  }

  @Test
  public void testJoin() {
    Collection<String> keys = new LinkedList<String>();
    keys.add("key1");
    keys.add("key2");
    keys.add("key3");
    assertEquals("key1,key2,key3", StringUtils.join(keys, ","));
  }
}
