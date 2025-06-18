/*
 * Copyright (Date see Readme), gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 */
package de.gematik.smartcards.sdcom.apdu.isoiec7816apdu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.Hex;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box testing of {@link EafiPasswordFormat}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.MethodNamingConventions", "checkstyle.methodname"})
final class TestEafiPasswordFormat {

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // intentionally empty
  } // end method */

  /** Method executed after other tests. */
  @AfterAll
  static void tearDownAfterClass() {
    // intentionally empty
  } // end method */

  /** Method executed before each test. */
  @BeforeEach
  void setUp() {
    // intentionally empty
  } // end method */

  /** Method executed after each test. */
  @AfterEach
  void tearDown() {
    // intentionally empty
  } // end method */

  /**
   * Randomly creation of an {@link EafiPasswordFormat#BCD} or {@link
   * EafiPasswordFormat#FORMAT_2_PIN_BLOCK}secret number of code points according to parameter.
   *
   * <p>The generated string contains the given number of decimal digits (i.e. {@code length}
   * decimal digits) plus any number of other Unicode Code Points at any position.
   *
   * @param length number of code points in secret
   * @return randomly generated secret
   */
  /* package */
  static String randomDigitSecret(final int length) {
    final List<Integer> result = new ArrayList<>();
    int currentLength = 0;

    for (; ; ) {
      final int nextCodePoint = RNG.nextCodePoint();

      if (('0' <= nextCodePoint) && (nextCodePoint <= '9')) {
        // ... decimal digit
        //     => increment currentLength
        currentLength++;
      } // end fi

      if (currentLength > length) {
        // ... length would be exceeded
        break;
      } // end fi

      result.add(nextCodePoint);
    } // end For (...)
    // ... list codePoints filled with #length of decimal digits and possibly other characters

    return new String(result.stream().mapToInt(i -> i).toArray(), 0, result.size());
  } // end method */

  /**
   * Randomly creation of an {@link EafiPasswordFormat#UTF8} secret number of code points according
   * to parameter.
   *
   * @param length number of code points in secret
   * @return randomly generated secret
   */
  /* package */
  static String randomUtf8Secret(final int length) {
    return RNG.nextUtf8(length);
  } // end method */

  /** Test method for {@link EafiPasswordFormat#BCD} {@link EafiPasswordFormat#blind(String)}. */
  @Test
  void test_Bcd_blind__String() {
    // Note: This simple method does not need extensive testing
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with manually chosen input
    // --- b. test with a bunch of randomly chosen secrets

    // --- a. smoke test with manually chosen input
    {
      assertEquals("*******f", EafiPasswordFormat.BCD.blind("1234567"));

      Map.ofEntries(
              Map.entry("", ""), // empty
              Map.entry("Alfred", ""), // no decimal digit
              Map.entry("Al0fr8e1d5", "****"), // even number of decimal digits
              Map.entry("3x", "*f") // odd number of decimal digits
              )
          .forEach(
              (input, expected) ->
                  assertEquals(
                      expected,
                      EafiPasswordFormat.BCD.blind(input),
                      input)); // end forEach((input, expected) -> ...)
    } // end --- a.

    // --- b. test with a bunch of randomly chosen secrets
    {
      final String expected = "***********************************************";
      final int maxLength = expected.length();

      IntStream.rangeClosed(0, maxLength)
          .forEach(
              length -> {
                final String secret = randomDigitSecret(length);

                assertEquals(
                    expected.substring(0, length) + ((1 == (length & 1)) ? "f" : ""),
                    EafiPasswordFormat.BCD.blind(secret));
              }); // end forEach(length -> ...)
    } // end --- b.
  } // end method */

  /**
   * Test method for {@link EafiPasswordFormat#BCD} and methods {@link
   * EafiPasswordFormat#octets(String)} and {@link EafiPasswordFormat#secret(byte[])}.
   */
  @Test
  void test_Bcd_octets_String() {
    // Note: This simple method does not need extensive testing
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with manually chosen input
    // --- b. test with a bunch of randomly chosen secrets

    // --- a. smoke test with manually chosen input
    {
      final String secretSmoke = "12345";
      final byte[] octetsSmoke = Hex.toByteArray("12345f");

      assertEquals(
          Hex.toHexDigits(octetsSmoke),
          Hex.toHexDigits(EafiPasswordFormat.BCD.octets(secretSmoke)));
      assertEquals(secretSmoke, EafiPasswordFormat.BCD.secret(octetsSmoke));

      Map.ofEntries(
              Map.entry("", List.of("")), // empty
              Map.entry("Matthias", List.of("")), // no decimal digit
              Map.entry("Ma0fr8e1d5", List.of("0815")), // even number of decimal digits
              Map.entry("3x", List.of("3f", "3")) // odd number of decimal digits
              )
          .forEach(
              (input, expected) -> {
                final String expOctets = expected.get(0);
                final String expSecret = expected.size() < 2 ? expOctets : expected.get(1);
                final byte[] octets = EafiPasswordFormat.BCD.octets(input);
                final String secret = EafiPasswordFormat.BCD.secret(octets);

                assertEquals(expOctets, Hex.toHexDigits(octets), input);
                assertEquals(expSecret, secret, input);
              }); // end forEach((input, expected) -> ...)
    } // end --- a.

    // --- b. test with a bunch of randomly chosen secrets
    IntStream.rangeClosed(0, 20)
        .forEach(
            length -> {
              final String input = randomDigitSecret(length);

              final List<Integer> cpSecret = new ArrayList<>();
              input
                  .codePoints()
                  .forEach(
                      nextCodePoint -> {
                        if (('0' <= nextCodePoint) && (nextCodePoint <= '9')) {
                          // ... decimal digit
                          //     => add to secret
                          cpSecret.add(nextCodePoint);
                        } // end fi
                      }); // end forEach(nextCodePoint -> ...)

              final String secret =
                  new String(cpSecret.stream().mapToInt(i -> i).toArray(), 0, cpSecret.size());
              final byte[] octets = EafiPasswordFormat.BCD.octets(input);

              assertEquals(secret + ((1 == (length & 1)) ? "f" : ""), Hex.toHexDigits(octets));
              assertEquals(secret, EafiPasswordFormat.BCD.secret(octets));
            }); // end forEach(length -> ...)
    // end --- b.
  } // end method */

  /**
   * Test method for {@link EafiPasswordFormat#FORMAT_2_PIN_BLOCK} {@link
   * EafiPasswordFormat#blind(String)}.
   */
  @Test
  void test_Format2PinBlock_blind__String() {
    final Map<Integer, String> expected =
        Map.ofEntries(
            Map.entry(0, "20ffffffffffffff"),
            Map.entry(1, "21*fffffffffffff"),
            Map.entry(2, "22**ffffffffffff"),
            Map.entry(3, "23***fffffffffff"),
            Map.entry(4, "24****ffffffffff"),
            Map.entry(5, "25*****fffffffff"),
            Map.entry(6, "26******ffffffff"),
            Map.entry(7, "27*******fffffff"),
            Map.entry(8, "28********ffffff"),
            Map.entry(9, "29*********fffff"),
            Map.entry(10, "2a**********ffff"),
            Map.entry(11, "2b***********fff"),
            Map.entry(12, "2c************ff"),
            Map.entry(13, "2d*************f"),
            Map.entry(14, "2e**************"));

    // Test strategy:
    // --- a. smoke test with manually chosen input
    // --- b. test with a bunch of randomly chosen secrets
    // --- c. ERROR: secret too long

    // --- a. smoke test with manually chosen input
    Map.ofEntries(
            Map.entry("", 0), // empty
            Map.entry("Ulla", 0), // no decimal digit
            Map.entry("Ul0fr8e1d5", 4), // even number of decimal digits
            Map.entry("3x", 1) // odd number of decimal digits
            )
        .forEach(
            (input, length) ->
                assertEquals(
                    expected.get(length),
                    EafiPasswordFormat.FORMAT_2_PIN_BLOCK.blind(input),
                    input)); // end forEach((input, expected) -> ...)
    // end --- a.

    // --- b. test with a bunch of randomly chosen secrets
    IntStream.rangeClosed(0, 14)
        .forEach(
            length -> {
              final String secret = randomDigitSecret(length);

              assertEquals(
                  expected.get(length), EafiPasswordFormat.FORMAT_2_PIN_BLOCK.blind(secret));
            }); // end forEach(length -> ...)
    // end --- b.

    // --- c. ERROR: secret too long
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> EafiPasswordFormat.FORMAT_2_PIN_BLOCK.blind("123456789012345"));

      assertEquals("secret too long", throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- c.
  } // end method */

  /**
   * Test method for {@link EafiPasswordFormat#FORMAT_2_PIN_BLOCK} and methods {@link
   * EafiPasswordFormat#octets(String)} and {@link EafiPasswordFormat#secret(byte[])}.
   */
  @Test
  void test_Format2PinBlock_octets_String() {
    // Note: This simple method does not need extensive testing
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with manually chosen input
    // --- b. test with a bunch of randomly chosen secrets
    // --- c. ERROR, octets(...): secret too long
    // --- d. ERROR, secret(...): invalid input-length
    // --- e. ERROR, secret(...): invalid 1st nibble
    // --- f. ERROR, secret(...): invalid 2nd nibble

    // --- a. smoke test with manually chosen input
    {
      final String secretSmoke = "1235";
      final byte[] octetsSmoke = Hex.toByteArray("241235ffffffffff");

      assertEquals(
          Hex.toHexDigits(octetsSmoke),
          Hex.toHexDigits(EafiPasswordFormat.FORMAT_2_PIN_BLOCK.octets(secretSmoke)));
      assertEquals(secretSmoke, EafiPasswordFormat.FORMAT_2_PIN_BLOCK.secret(octetsSmoke));

      Map.ofEntries(
              Map.entry("", List.of("20ffffffffffffff", "")), // empty
              Map.entry("Sabine", List.of("20ffffffffffffff", "")), // no decimal digit
              Map.entry("Sa0fr8e1d5", List.of("240815ffffffffff", "0815")), // even number of digits
              Map.entry("3x", List.of("213fffffffffffff", "3")) // odd number of decimal digits
              )
          .forEach(
              (input, expected) -> {
                final String expOctets = expected.get(0);
                final String expSecret = expected.get(1);
                final byte[] octets = EafiPasswordFormat.FORMAT_2_PIN_BLOCK.octets(input);
                final String secret = EafiPasswordFormat.FORMAT_2_PIN_BLOCK.secret(octets);

                assertEquals(expOctets, Hex.toHexDigits(octets), input);
                assertEquals(expSecret, secret, input);
              }); // end forEach((input, expected) -> ...)
    } // end --- a.

    // --- b. test with a bunch of randomly chosen secrets
    IntStream.rangeClosed(0, 14)
        .forEach(
            length -> {
              final String input = randomDigitSecret(length);

              final List<Integer> cpSecret = new ArrayList<>();
              input
                  .codePoints()
                  .forEach(
                      nextCodePoint -> {
                        if (('0' <= nextCodePoint) && (nextCodePoint <= '9')) {
                          // ... decimal digit
                          //     => add to secret
                          cpSecret.add(nextCodePoint);
                        } // end fi
                      }); // end forEach(nextCodePoint -> ...)

              final String secret =
                  new String(cpSecret.stream().mapToInt(i -> i).toArray(), 0, cpSecret.size());
              final byte[] octets = EafiPasswordFormat.FORMAT_2_PIN_BLOCK.octets(input);

              assertEquals(
                  String.format("2%x%sffffffffffffff", length, secret).substring(0, 16),
                  Hex.toHexDigits(octets));
              assertEquals(secret, EafiPasswordFormat.FORMAT_2_PIN_BLOCK.secret(octets));
            }); // end forEach(length -> ...)
    // end --- b.

    // --- c. ERROR, octets(...): secret too long
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> EafiPasswordFormat.FORMAT_2_PIN_BLOCK.octets("123456789012345"));

      assertEquals("secret too long", throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- c.

    // --- d. ERROR: invalid input-length
    IntStream.rangeClosed(0, 20)
        .filter(length -> length != 8)
        .forEach(
            length -> {
              final Throwable throwable =
                  assertThrows(
                      IllegalArgumentException.class,
                      () -> EafiPasswordFormat.FORMAT_2_PIN_BLOCK.secret(RNG.nextBytes(length)));

              assertEquals("invalid input-length", throwable.getMessage());
              assertNull(throwable.getCause());
            }); // end forEach(length -> ...)
    // end --- d.

    // --- e. ERROR: invalid 1st nibble
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  EafiPasswordFormat.FORMAT_2_PIN_BLOCK.secret(
                      Hex.toByteArray("1e22334455667788")));

      assertEquals("type indicator != 2", throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- e.

    // --- f. ERROR: invalid 2nd nibble
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  EafiPasswordFormat.FORMAT_2_PIN_BLOCK.secret(
                      Hex.toByteArray("2f22334455667788")));

      assertEquals("invalid 2nd nibble", throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- f.
  } // end method */

  /** Test method for {@link EafiPasswordFormat#UTF8} {@link EafiPasswordFormat#blind(String)}. */
  @Test
  void test_Utf8_blind__String() {
    // Note: This simple method does not need extensive testing
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with manually chosen input
    // --- b. test with a bunch of randomly chosen secrets

    // --- a. smoke test with manually chosen input
    {
      assertEquals("****", EafiPasswordFormat.UTF8.blind("1234"));

      Map.ofEntries(Map.entry("", ""), Map.entry("Alfred4711", "**********"))
          .forEach(
              (input, expected) -> assertEquals(expected, EafiPasswordFormat.UTF8.blind(input)));
    } // end --- a.

    // --- b. test with a bunch of randomly chosen secrets
    final String expected = "*******************************************";
    final int maxLength = expected.length();
    IntStream.rangeClosed(0, maxLength)
        .forEach(
            length -> {
              final String secret = randomUtf8Secret(length);

              assertEquals(expected.substring(0, length), EafiPasswordFormat.UTF8.blind(secret));
            }); // end forEach(length -> ...)
    // end --- b.
  } // end method */

  /**
   * Test method for {@link EafiPasswordFormat#UTF8} and methods {@link
   * EafiPasswordFormat#octets(String)} and {@link EafiPasswordFormat#secret(byte[])}.
   */
  @Test
  void test_Utf8_octets_String() {
    // Note: This simple method does not need extensive testing
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. Test with a bunch of randomly chosen secrets

    // --- a. smoke test
    {
      final String secret = "1234";
      final byte[] octets = secret.getBytes(StandardCharsets.UTF_8);

      assertEquals(
          Hex.toHexDigits(octets), Hex.toHexDigits(EafiPasswordFormat.UTF8.octets(secret)));
      assertEquals(secret, EafiPasswordFormat.UTF8.secret(octets));
    } // end --- a.

    // --- b. Test with a bunch of randomly chosen secrets
    RNG.intsClosed(0, 20, 10)
        .forEach(
            length -> {
              final String secret = randomUtf8Secret(length);
              final byte[] octets = secret.getBytes(StandardCharsets.UTF_8);

              assertEquals(
                  Hex.toHexDigits(octets), Hex.toHexDigits(EafiPasswordFormat.UTF8.octets(secret)));
              assertEquals(secret, EafiPasswordFormat.UTF8.secret(octets));
            }); // end forEach(length -> ...)
    // end --- b.
  } // end method */
} // end class
