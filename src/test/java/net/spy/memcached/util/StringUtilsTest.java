package net.spy.memcached.util;

import net.spy.memcached.MemcachedClientIF;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the correct functionality of the {@link StringUtils} class.
 */
public class StringUtilsTest {

  /**
   * A dummy key which is longer than allowed.
   */
  private String tooLongKey;

  @Before
  public void setup() {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < MemcachedClientIF.MAX_KEY_LENGTH+1; i++) {
      builder.append("k");
    }
    tooLongKey = builder.toString();
  }

  @Test
  public void shouldJoinChunk() {
    List<String> chunks = Arrays.asList("chunk1");
    String delimiter = ",";

    String expected = "chunk1";
    String result = StringUtils.join(chunks, delimiter);
    assertEquals(expected, result);
  }

  @Test
  public void shouldJoinChunks() {
    List<String> chunks = Arrays.asList("chunk1", "chunk2", "chunk3");
    String delimiter = ",";

    String expected = "chunk1,chunk2,chunk3";
    String result = StringUtils.join(chunks, delimiter);
    assertEquals(expected, result);
  }

  @Test
  public void shouldReturnEmptyStringWithNoChunks() {
    String result = StringUtils.join(Collections.<String>emptyList(), ",");
    assertEquals("", result);
  }

  @Test
  public void shouldValidateJSONObject() {
    assertTrue(StringUtils.isJsonObject("{\"a\":true}"));
    assertTrue(StringUtils.isJsonObject("[0, 1, 2]"));
    assertTrue(StringUtils.isJsonObject("true"));
    assertTrue(StringUtils.isJsonObject("false"));
    assertTrue(StringUtils.isJsonObject("null"));
    assertTrue(StringUtils.isJsonObject("1"));
    assertTrue(StringUtils.isJsonObject("-1"));
  }

  @Test
  public void shouldValidateNonJSONObject() {
    assertFalse(StringUtils.isJsonObject("foobar"));
    assertFalse(StringUtils.isJsonObject("0.5"));
    assertFalse(StringUtils.isJsonObject("1,244.0"));
  }

  @Test
  public void shouldValidateAsciiKey() {
    StringUtils.validateKey("mykey1234", false);
    assertTrue(true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailValidationWithAsciiSpace() {
    StringUtils.validateKey("key baz", false);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailValidationWithAsciiNewline() {
    StringUtils.validateKey("keybaz\n", false);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailValidationWithAsciiReturn() {
    StringUtils.validateKey("keybaz\r", false);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailValidationWithAsciiNull() {
    StringUtils.validateKey("keybaz\0", false);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailValidationWithAsciiTooLong() {
    StringUtils.validateKey(tooLongKey, false);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailValidationWithBinaryTooLong() {
    StringUtils.validateKey(tooLongKey, true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailValidationWithAsciiEmpty() {
    StringUtils.validateKey("", false);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailValidationWithBinaryEmpty() {
    StringUtils.validateKey("", true);
  }

  @Test
  public void shouldValidateBinaryKey() {
    StringUtils.validateKey("mykey1234", true);
    StringUtils.validateKey("key baz", true);
    StringUtils.validateKey("keybaz\n", true);
    StringUtils.validateKey("keybaz\r", true);
    StringUtils.validateKey("keybaz\0", true);
  }

}
